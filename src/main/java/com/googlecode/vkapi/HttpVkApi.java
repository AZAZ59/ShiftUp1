package com.googlecode.vkapi;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import com.googlecode.vkapi.convert.JsonConverter;
import com.googlecode.vkapi.domain.OAuthToken;
import com.googlecode.vkapi.domain.error.VkErrorResponse;
import com.googlecode.vkapi.domain.group.VkGroup;
import com.googlecode.vkapi.domain.message.VkWallMessage;
import com.googlecode.vkapi.domain.user.GroupUsers;
import com.googlecode.vkapi.domain.user.VkUser;
import com.googlecode.vkapi.exceptions.VkException;
import com.googlecode.vkapi.exceptions.VkExceptions;
import scala.Int;

/**
 * HttpClient based implementation for {//@link VkApi}. For initializing, needs
 * properties appId, appKey - obtained from vk.com application settings and
 * responseUri to which vk.com will send code for further authentication
 * <p>
 * //@author Alexey Grigorev
 * //@see VkApi
 */
public class HttpVkApi implements VkApi {

    public static final String[] APP_SCOPES = {"friends", "wall", "groups", "offline"};
    public static final String[] USER_FIELDS = {"uid", "first_name", "last_name", "photo", "bdate"};

    private UriCreator uriCreator = new UriCreator();
    private HttpClientWrapper httpClient = new HttpClientWrapper();
    private JsonConverter jsonConverter = JsonConverter.INSTANCE;

    private final String appId;
    private final String appKey;
    private final String responseUri;

    /**
     * //@param appId application id
     * //@param appKey application key
     * //@param responseUri url for sending the code
     */
    public HttpVkApi(String appId, String appKey, String responseUri) {
        this.appId = appId;
        this.appKey = appKey;
        this.responseUri = responseUri;
    }

    //@Override
    public String getAuthUri() {
        return uriCreator.authUri(appId, APP_SCOPES, responseUri);
    }

    //@Override
    public OAuthToken authUser(String code) throws VkException {
        Validate.notNull(code, "Expected code not to be null");

        String accessTokenUri = uriCreator.accessTokenUri(appId, appKey, code);

        String json = executeAndProcess(accessTokenUri, null);
        return jsonConverter.jsonToAuthToken(json);
    }

    //@Override
    public VkUser currentUserInfo(OAuthToken authToken) throws VkException {
        String uri = uriCreator.userInfoUri(authToken.getUserId(), USER_FIELDS, authToken);
        String json = executeAndProcess(uri, authToken);
        List<VkUser> result = jsonConverter.jsonToUserList(json);
        return firstOrNull(result);
    }

    public VkUser userInfo(int id, OAuthToken authToken) throws VkException {
        String uri = uriCreator.userInfoUri(id, USER_FIELDS, authToken);
        String json = executeAndProcess(uri, authToken);
        List<VkUser> result = jsonConverter.jsonToUserList(json);
        return firstOrNull(result);
    }

    private static <E> E firstOrNull(List<E> list) {
        return list.isEmpty() ? null : list.get(0);
    }

    private String executeAndProcess(String uri, OAuthToken authToken) throws VkException {
        String json = httpClient.executeGet(uri);

        while (StringUtils.containsIgnoreCase(json, "Too many requests per second")) {
            try {
                Thread.sleep(1000/3);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            json = httpClient.executeGet(uri);
        }

        if (StringUtils.startsWith(json, "{\"error\":")) {
            VkErrorResponse error = jsonConverter.jsonToVkError(json);
            VkExceptions.throwAppropriate(error, authToken);
        }

        return json;
    }

    //@Override
    public Collection<VkUser> getFriends(OAuthToken token) throws VkException {
        return getFriends(token.getUserId(), token);
    }

    //@Override
    public Collection<VkUser> getFriends(int userId, OAuthToken authToken) throws VkException {
        String uri = uriCreator.userFriendsUri(userId, USER_FIELDS, authToken);
        String json = executeAndProcess(uri, authToken);
        return jsonConverter.jsonToUserList(json);
    }

    //@Override
    public Collection<VkWallMessage> lastGroupWallMessages(long groupId, WallFiler filter, OAuthToken authToken)
            throws VkException {
        return lastGroupWallMessages(groupId, filter, 0, authToken);
    }

    //@Override
    public Collection<VkWallMessage> lastGroupWallMessages(long groupId, WallFiler filter, int limit,
                                                           OAuthToken authToken) throws VkException {
        int num_threads = 20;
        int step = 100;
        Collection<VkWallMessage> list = Collections.synchronizedCollection(new LinkedList<>());
        ExecutorService es = Executors.newFixedThreadPool(num_threads);
        int postCount = getPostCount(groupId, authToken);

        for(int offset=0;offset<Math.min(postCount,limit);offset+=step){
            es.submit(new Worker(groupId, filter, authToken, list, step, offset,postCount));
        }

        es.shutdown();
        try {System.err.println(es.awaitTermination(1, TimeUnit.HOURS));} catch (InterruptedException e) {e.printStackTrace();}
        return list;
    }

    public Collection<VkWallMessage> lastGroupWallMessages(String domain, WallFiler filter, int limit,
                                                           OAuthToken authToken) throws VkException {
        int num_threads = 20;
        int step = 100;
        Collection<VkWallMessage> list = Collections.synchronizedCollection(new LinkedList<>());

        ExecutorService es = Executors.newFixedThreadPool(num_threads);
        int postCount = getPostCount(domain, authToken);

        for(int offset=0;offset<postCount;offset+=step){
            es.submit(new Worker(domain, filter, authToken, list, step, offset,postCount));
        }

        es.shutdown();
        try {System.err.println(es.awaitTermination(48, TimeUnit.HOURS));} catch (InterruptedException e) {e.printStackTrace();}
        return list;
    }

    private void wait100() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    public int getPostCount(long groupId, OAuthToken authToken) throws VkException {
        return jsonConverter.jsonToWallCount(executeAndProcess(uriCreator.groupWallMessages(groupId, WallFiler.ALL, 0, 0, authToken), authToken));
    }
    public int getPostCount(String groupId, OAuthToken authToken) throws VkException {
        return jsonConverter.jsonToWallCount(executeAndProcess(uriCreator.wallMessages(groupId, WallFiler.ALL, 0, 0, authToken), authToken));
    }


    //@Override
    public Set<Integer> mutualFriends(int user1Id, int user2Id, OAuthToken authToken) throws VkException {
        String uri = uriCreator.mutualFriends(user1Id, user2Id, authToken);
        String json = executeAndProcess(uri, authToken);
        return jsonConverter.jsonToIntegerSet(json);
    }

    //@Override
    public VkGroup groupInfo(long groupId, OAuthToken authToken) throws VkException {
        String uri = uriCreator.groupInfo(groupId, authToken);
        String json = executeAndProcess(uri, authToken);
        List<VkGroup> result = jsonConverter.jsonToVkGroups(json);
        return firstOrNull(result);
    }

    public Collection<VkGroup> groupsOfUser(long userId, OAuthToken authToken) throws VkException {
        int count = 1000, offset = 0;
        //int total = userInfo((int) userId,authToken).;
        String uri = uriCreator.groupList(userId, authToken);
        String json = executeAndProcess(uri, authToken);
        return jsonConverter.jsonToVkGroups(json);
    }

    //@Override
    public Set<Integer> groupUsers(long groupId, OAuthToken authToken) throws VkException {
        int count = 1000, offset = 0;

        GroupUsers groupUsers = extractNextUsersFromGroup(groupId, authToken, count, offset);

        if (groupUsers.allExtracted()) {
            return groupUsers.getUsers();
        } else {
            return theRestOfUsers(groupId, authToken, count, groupUsers);
        }
    }

    public Set<Integer> groupUsers(VkGroup group, OAuthToken authToken) throws VkException {
        int count = 1000, offset = 0;
        Set<Integer> set = Collections.synchronizedSet(new HashSet<>());

        ExecutorService es = Executors.newFixedThreadPool(20);

        for(offset=0;offset<=group.getMembersCount();offset+=count){
            final int finalOffset = offset;
            es.submit(() -> {
                set.addAll(extractNextUsersFromGroup(group.getGroupId(),authToken,1000, finalOffset).getUsers());
                return 0;
            });
        }
        es.shutdown();
        try {
            es.awaitTermination(1,TimeUnit.DAYS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return set;
    }

    private GroupUsers extractNextUsersFromGroup(long groupId, OAuthToken authToken, int count, int offset)
            throws VkException {
        String uri = uriCreator.groupUsers(groupId, count, offset, authToken);
        String json = "";
        boolean isEnd = false;
        while (!isEnd) {
            try {
                json = executeAndProcess(uri, authToken);
                isEnd = true;
            } catch (VkException e) {
                System.err.println(offset + " " + count);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        }
        return jsonConverter.jsonToGroupUsers(json);
    }

    private Set<Integer> theRestOfUsers(long groupId, OAuthToken authToken, int step, GroupUsers groupUsers)
            throws VkException {
        Set<Integer> result = new LinkedHashSet<Integer>(groupUsers.getUsers());
        int totalCount = groupUsers.getTotalCount(), extracted = step;

        while (extracted < totalCount) {
            GroupUsers next = extractNextUsersFromGroup(groupId, authToken, step, extracted);
            result.addAll(next.getUsers());
            extracted = extracted + step;
        }

        return result;
    }


    void setUriCreator(UriCreator uriCreator) {
        this.uriCreator = uriCreator;
    }

    void setHttpClient(HttpClientWrapper httpClient) {
        this.httpClient = httpClient;
    }

    void setJsonConverter(JsonConverter jsonConverter) {
        this.jsonConverter = jsonConverter;
    }

    private class Worker implements Runnable {
        private long groupId;
        private WallFiler filter;
        private OAuthToken authToken;
        private Collection<VkWallMessage> list;
        private int step;
        private int offset;
        private String domain;
        private long count;
        public Worker(long groupId, WallFiler filter, OAuthToken authToken, Collection<VkWallMessage> list, int step, int offset,long postCount) {
            this.groupId = groupId;
            this.filter = filter;
            this.authToken = authToken;
            this.list = list;
            this.step = step;
            this.offset = offset;
            this.domain="";
            this.count=postCount;
        }

        public Worker(String domain, WallFiler filter, OAuthToken authToken, Collection<VkWallMessage> list, int step, int offset, int postCount) {
            this.domain=domain;
            this.groupId=-1;
            this.filter = filter;
            this.authToken = authToken;
            this.list = list;
            this.step = step;
            this.offset = offset;
            this.domain="";
            this.count=postCount;
        }

        @Override
        public void run() {
            long l=System.currentTimeMillis();
            String uri = uriCreator.groupWallMessages(groupId, filter, step, offset, authToken);
            if(groupId==-1){
                uri=uriCreator.wallMessages(domain,filter, step, offset,authToken);
            }
            String json = null;
            if(offset%500==0){
                System.out.println("     "+
                        new SimpleDateFormat("hh:mm:ss").format(new Date())+
                        " For group "+groupId+
                        " from " + offset +
                        " to " + (offset + step)+
                        " of "+count);
            }
            try {
                json = executeAndProcess(uri, authToken);
            } catch (VkException e) {
                e.printStackTrace();
            }
            Collection<VkWallMessage> coll = jsonConverter.jsonToWallMessage(json);
            list.addAll(coll);
            //System.out.println("END from " + offset + " to " + (offset + step)+" time: "+(System.currentTimeMillis()-l));

        }

    }
}
