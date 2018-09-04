/*
 * MIT License
 *
 * Copyright (c) 2017-2018 talust.org talust.io
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package org.talust.controller;

import com.alibaba.fastjson.JSONObject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.talust.common.tools.ArithUtils;
import org.talust.common.tools.FileUtil;
import org.talust.core.model.Account;
import org.talust.core.model.Address;
import org.talust.core.network.MainNetworkParams;
import org.talust.core.storage.AccountStorage;
import org.talust.core.storage.TransactionStorage;

import java.io.File;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/user")
@Api("帐户相关的Api")
@Slf4j
public class AccountController {

    @ApiOperation(value = "查询单一地址拥有的代币", notes = "查询拥有的代币")
    @PostMapping(value = "getCoins")
    JSONObject getCoins(@RequestParam String address) {
        JSONObject jsonObject = new JSONObject();
        Address addr = Address.fromBase58(MainNetworkParams.get(), address);
        if (AccountStorage.get().reloadCoin()) {
            long value = TransactionStorage.get().getBalanceAndUnconfirmedBalance(addr.getHash160())[0].value;
            long lockValue = TransactionStorage.get().getBalanceAndUnconfirmedBalance(addr.getHash160())[1].value;
            jsonObject.put("value", ArithUtils.div(value + "", "100000000", 8));
            jsonObject.put("lockValue", ArithUtils.div(lockValue + "", "100000000", 8));
        }
        return jsonObject;
    }

    @ApiOperation(value = "账户加密", notes = "账户加密")
    @PostMapping(value = "encryptWallet")
    JSONObject encryptWallet(@RequestParam String address, @RequestParam String password) {
        return AccountStorage.get().encryptWallet(password, address);
    }

    @ApiOperation(value = "查询全部地址拥有的代币", notes = "查询拥有的代币")
    @PostMapping(value = "getAllCoins")
    JSONObject getAllCoins() {
        JSONObject jsonObject = new JSONObject();
        List<Account> accountList = AccountStorage.get().getAccountList();
        for (Account account : accountList) {
            if (AccountStorage.get().reloadCoin()) {
                JSONObject data = new JSONObject();
                long value = TransactionStorage.get().getBalanceAndUnconfirmedBalance(account.getAddress().getHash160())[0].value;
                long lockValue = TransactionStorage.get().getBalanceAndUnconfirmedBalance(account.getAddress().getHash160())[1].value;
                data.put("value", ArithUtils.div(value + "", "100000000", 8));
                data.put("lockValue", ArithUtils.div(lockValue + "", "100000000", 8));
                jsonObject.put(account.getAddress().getBase58(), data);
            }
        }
        return jsonObject;
    }

    @ApiOperation(value = "导入账户", notes = "导入账户")
    @PostMapping(value = "importAccountFile")
    JSONObject importAccountFile(@RequestParam String path) {
        //验证文件是否存在
        if (null == path) {
            //路径为空
        }
        File file = new File(path);
        if (!file.exists()) {
            //文件不存在
        }
        try {
            String   content = FileUtil.fileToTxt(file);
            JSONObject fileJson = JSONObject.parseObject(content);
            Account account = Account.parse(fileJson.getBytes("data"), 0, MainNetworkParams.get());
            try {
                account.verify();
                AccountStorage.get().importAccountFile(account);
            } catch (Exception e) {
                log.warn("默认登陆{}时出错", account.getAddress().getBase58(), e);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
