package com.shuangyueliao.chat;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.shuangyueliao.chat.entity.Account;
import com.shuangyueliao.chat.mapper.AccountMapper;
import com.shuangyueliao.chat.queue.custom.CustomOfflineInfoHelper;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.Map;

/**
 * WebSocket聊天室，客户端参考docs目录下的websocket.html
 */
@SpringBootTest
@RunWith(SpringRunner.class)
public class HappyChatSpringBootMainTest {
    @Autowired
    private AccountMapper accountMapper;
    @Test
    public void testMysql() {
        Account account = accountMapper.selectById(1);
        System.out.println(account);
        Assert.assertNotNull(account);
    }
    @Test
    public void hi() {
        Map<Integer, List<String>> infoMap = CustomOfflineInfoHelper.infoMap;
        Map<String, List<Integer>> groupMap = CustomOfflineInfoHelper.groupMap;
        System.out.println("finish");
        System.exit(0);
    }

    @Test
    public void testQueueProducer() throws Exception {
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.select("distinct groupNumber");
        List list = accountMapper.selectObjs(queryWrapper);
        System.out.println("finish");
    }
}
