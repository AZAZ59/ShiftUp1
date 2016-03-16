package ru.azaz.ratioFunctions;

import com.googlecode.vkapi.HttpVkApi;
import com.googlecode.vkapi.domain.VkOAuthToken;
import com.googlecode.vkapi.domain.group.VkGroup;
import com.googlecode.vkapi.exceptions.VkException;
import com.sun.org.apache.xpath.internal.SourceTree;
import ru.azaz.VkFile;
import ru.azaz.utils.Intersection;
import ru.azaz.vkGetter.Tuple;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Created by AZAZ on 09.03.2016.
 */
public class ShiftUp extends VkFile implements RatioFunction {
    FutureTask<Map<VkGroup, Integer>> map;

    Tuple<Map<VkGroup, Integer>, Integer> numOfIntersectGroup(long grid, int limit) {
        Map<VkGroup, Integer> ans = Collections.synchronizedMap(new HashMap<VkGroup, Integer>());
        VkGroup gr;
        ExecutorService es = Executors.newFixedThreadPool(20);
        AtomicInteger count =new AtomicInteger(0);
        try {
            gr = vkApi.groupInfo(grid, tok);
            Set<Integer> members = vkApi.groupUsers(gr, tok);
            AtomicInteger i= new AtomicInteger(0);
            if(limit<0){
                limit=members.size();
            }
            members.stream().limit(limit).forEach((memberId) -> {
                es.submit((Runnable) () -> {
                    try {
                        int t=i.addAndGet(1);
                        Collection<VkGroup> groups=vkApi.groupsOfUser(memberId, tok);
                        if(groups.size()>0){
                            groups.forEach(group -> {
                                if (!ans.containsKey(group)) ans.put(group, 0);
                                ans.put(group, ans.get(group) + 1);
                            });
                            count.incrementAndGet();
                        }
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

        return new Tuple<>(ans,count.get());
    }
    Tuple<Map<VkGroup, Integer>, Integer> numOfIntersectGroup(String group,int limit){
        try {
            VkGroup gr=vkApi.groupInfo(group,tok);
            return numOfIntersectGroup(gr.getGroupId(),limit);
        } catch (VkException e) {
            e.printStackTrace();
        }
        return null;
    }
    Tuple<Map<VkGroup, Integer>, Integer> numOfIntersectGroup(String group){
        return numOfIntersectGroup(group,-1);
    }
    Tuple<Map<VkGroup, Integer>, Integer> numOfIntersectGroup(long group){
        return numOfIntersectGroup(group,-1);
    }

    @Override
    public Future<Double> apply(VkGroup group, VkGroup group2) {
        return new FutureTask<>(() -> (double) Intersection.intersect(group.getMembers(vkApi, tok), group2.getMembers(vkApi, tok)).size());
    }

    @Override
    public void init(VkGroup group) {
        map=new  FutureTask<>(() ->numOfIntersectGroup(group.getGroupId()).val1);
        map.run();
    }
}
