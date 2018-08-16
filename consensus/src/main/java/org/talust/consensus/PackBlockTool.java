package org.talust.consensus;

import lombok.extern.slf4j.Slf4j;
import org.talust.common.crypto.Base58;
import org.talust.common.crypto.Sha256Hash;
import org.talust.common.model.*;
import org.talust.common.model.Message;
import org.talust.common.tools.*;
import org.talust.core.core.Definition;
import org.talust.core.core.NetworkParams;
import org.talust.core.data.ConsensusCalculationUtil;
import org.talust.core.data.DataContainer;
import org.talust.core.model.*;
import org.talust.common.model.DepositAccount;
import org.talust.core.network.MainNetworkParams;
import org.talust.core.script.ScriptBuilder;
import org.talust.core.storage.*;
import org.talust.core.transaction.Transaction;
import org.talust.core.transaction.TransactionInput;
import org.talust.network.netty.ConnectionManager;
import org.talust.network.netty.queue.MessageQueue;

import java.util.ArrayList;
import java.util.List;

/**
 * 打包工具
 */
@Slf4j
public class PackBlockTool {
    private DataContainer dataContainer = DataContainer.get();
    private BlockStorage blockStorage = BlockStorage.get();
    private NetworkParams networkParams = MainNetworkParams.get();
    private CacheManager cu = CacheManager.get();


    //打包
    public void  pack(int packageTime) {
        try {
            Account account = AccountStorage.get().getAccount();
            //批量获取需要打包的数据
            List<Transaction> transactionList = new ArrayList<>();
            long height = blockStorage.getBestBlockHeader().getBlockHeader().getHeight();
            height++;
            Transaction coinBase = getCoinBase(packageTime, height);
            if (coinBase != null) {
                //加入挖矿奖励,挖矿交易生成
                transactionList.add(coinBase);
            }
            transactionList.addAll( dataContainer.getBatchRecord());
            //本地最新区块
            BlockHeader BlockHeader = blockStorage.getBestBlockHeader().getBlockHeader();
            //获取我的时段开始时间
            Block block = new Block(networkParams);
            long currentHeight = BlockHeader.getHeight() + 1;
            block.setHeight(currentHeight);
            block.setPreHash(BlockHeader.getHash());
            block.setTime(packageTime);
            block.setVersion(networkParams.getProtocolVersionNum(NetworkParams.ProtocolVersion.CURRENT));
            block.setTxs(transactionList);
            block.setTxCount(transactionList.size());
            block.setMerkleHash(block.buildMerkleHash());
            block.sign(account);
            block.verify();
            block.verifyScript();
            BlockStore blockStore = new BlockStore(networkParams, block);
            byte[] data = SerializationUtil.serializer(blockStore);
            byte[] sign = account.getEcKey().sign(Sha256Hash.of(data)).encodeToDER();

            Message message = new Message();
            message.setTime(packageTime);
            //对于接收方来说,是区块到来,因为此消息有明确的接收方
            message.setType(MessageType.BLOCK_ARRIVED.getType());
            message.setContent(data);
            message.setSigner(account.getEcKey().getPubKey());
            message.setSignContent(sign);

            MessageChannel mc = new MessageChannel();
            mc.setMessage(message);
            mc.setFromIp(ConnectionManager.get().selfIp);
            MessageQueue.get().addMessage(mc);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取挖矿奖励
     *
     * @param packageTime
     * @param height
     * @return
     */
    private Transaction getCoinBase(int packageTime, long height) throws Exception {
        //添加共识奖励交易
        Transaction coinBase = new Transaction(networkParams);
        coinBase.setVersion(Definition.VERSION);
        coinBase.setType(Definition.TYPE_COINBASE);
        TransactionInput input = new TransactionInput();
        coinBase.addInput(input);
        input.setScriptSig(ScriptBuilder.createCoinbaseInputScript("this a coinBase tx".getBytes()));
        Coin consensusRreward = ConsensusCalculationUtil.calculatConsensusReward(height);
        Coin minerRreward = ConsensusCalculationUtil.calculatMinerReward(height);
        List<String>  miningAddress = CacheManager.get().get(new String(Constant.MINING_ADDRESS));
        if (miningAddress == null) {
            return null;
        }
        int size = miningAddress.size();
        //当前获得收益的区块
        int idx = (int) (height % size);
        //矿机自身获得
        byte[] sn =Base58.decode(miningAddress.get(idx));
        coinBase.addOutput(minerRreward,new Address(networkParams,sn));
        log.info("挖矿奖励给地址:{},高度:{},金额:{}", sn, height, minerRreward.value);

        List<DepositAccount> deposits = ChainStateStorage.get().getDeposits(sn);
        //当前没有储蓄帐户,则挖出来的币直接奖励给矿机
        if (deposits.size() == 0) {
            coinBase.addOutput(consensusRreward,new Address(networkParams,sn));
            log.info("挖矿奖励给储蓄地址:{},高度:{},金额:{}", sn, height,consensusRreward.value);
        } else {//有储蓄者
            Coin totalAmount = calTotalAmount(deposits);
            for (DepositAccount deposit : deposits) {
                Coin per =consensusRreward.multiply(deposit.getAmount().divide(totalAmount));
                coinBase.addOutput(per,new Address(networkParams,deposit.getAddress()));
                log.info("挖矿奖励给储蓄地址:{},高度:{},金额:{}", Base58.encode(deposit.getAddress()),height, per.value);
            }
        }
        coinBase.verify();
        return coinBase;
    }

    private Coin calTotalAmount(List<DepositAccount> deposits) {
        Coin total =  Coin.ZERO;
        for (DepositAccount deposit : deposits) {
            total = total.add(deposit.getAmount());
        }
        return total;
    }

//    public static void main(String[] args) {
//        BigDecimal divide = new BigDecimal(10).divide(new BigDecimal(3), 8, BigDecimal.ROUND_HALF_UP);
//        System.out.println(divide.doubleValue());
//    }

}
