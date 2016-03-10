package ru.azaz.vkGetter;

import com.googlecode.vkapi.HttpVkApi;
import com.googlecode.vkapi.domain.VkOAuthToken;
import com.googlecode.vkapi.domain.group.VkGroup;
import com.googlecode.vkapi.domain.group.VkGroupBuilder;
import com.googlecode.vkapi.exceptions.VkException;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by AZAZ on 09.03.2016.
 */
public class ShiftUp {
    Map<VkGroup, Integer> numOfIntersectGroup(long grid, HttpVkApi vkApi, VkOAuthToken tok) {
        Map<VkGroup, Integer> ans = Collections.synchronizedMap(new HashMap<VkGroup, Integer>());
        VkGroup gr = null;
        ExecutorService es = Executors.newFixedThreadPool(20);
        try {
            gr = vkApi.groupInfo(grid, tok);
            System.out.println(gr.getMembersCount());
            Set<Integer> members = vkApi.groupUsers(gr, tok);
            AtomicInteger i= new AtomicInteger(0);
            members.stream().forEach((memberId) -> {

                es.submit((Runnable) () -> {
                    try {
                        int t=i.addAndGet(1);
                        System.out.println("start Analize "+memberId +" "+(t)+" from "+members.size());
                        vkApi.groupsOfUser(memberId, tok).forEach(group -> {
                            if (!ans.containsKey(group)) ans.put(group, 0);
                            ans.put(group, ans.get(group) + 1);
                        });
                        System.out.println("end Analize "+memberId +" "+t+" from "+members.size());
                    } catch (VkException e) {
                        e.printStackTrace();
                    }
                });
            });

            es.shutdown();
            es.awaitTermination(1, TimeUnit.DAYS);
        } catch (VkException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return ans;
    }
}
