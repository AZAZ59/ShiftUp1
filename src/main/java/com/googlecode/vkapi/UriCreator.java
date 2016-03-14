package com.googlecode.vkapi;

import org.apache.commons.lang3.StringUtils;

import com.googlecode.vkapi.domain.OAuthToken;

/**
 * Creates urls for accessing VK.com api fuctions
 * 
 * @author Alexey Grigorev
 */

class UriCreator {

    private static final String METHOD_URI = "https://api.vk.com/method/";

    /**
     * Generates uri for sending authorization requests
     * 
     * @param appId id of the application
     * @param scopes needed functions (such as "friends", "messages", etc)
     * @param responseUri the uri to which the response will be sent
     * @return uri to be shown to the user to authorization
     */
    public String authUri(String appId, String[] scopes, String responseUri) {
        return "http://oauth.vk.com/authorize?" + 
                "client_id=" + appId + "&" + 
                "scope=" + StringUtils.join(scopes, ",") + "&" + 
                "redirect_uri=" + responseUri + "&" + 
                "response_type=token";
    }

    public String accessTokenUri(String appId, String appKey, String code) {
        return "https://oauth.vk.com/access_token?client_id=" + appId + "&client_secret=" + appKey + "&code=" + code;
    }

    public String userInfoUri(int vkUserId, String[] fields, OAuthToken authToken) {
        return METHOD_URI + "users.get?" + 
                "uids=" + vkUserId + "&" + 
                "fields=" + StringUtils.join(fields, ",") + "&" + 
                "access_token=" + authToken.getAccessToken();
    }

    public String userFriendsUri(int vkUserId, String[] fields, OAuthToken authToken) {
        return METHOD_URI + "friends.get?" + 
                "uid=" + vkUserId + "&" + 
                "fields=" + StringUtils.join(fields, ",") + "&" + 
                "access_token=" + authToken.getAccessToken();
    }

    public String groupWallMessages(long groupId, WallFiler filter, int limit, OAuthToken authToken) {
        // minus indicates that the id belongs to a group
        return wallMessages(-groupId, filter, limit,0, authToken);
    }
    public String groupWallMessages(long groupId, WallFiler filter, int limit,int offset, OAuthToken authToken) {
        // minus indicates that the id belongs to a group
        return wallMessages(-groupId, filter, limit,offset, authToken);
    }
    
    public String wallMessages(long userId, WallFiler filter, int limit, OAuthToken authToken) {
        return wallMessages(userId,filter,limit,0,authToken);
    }

    public String wallMessages(long userId, WallFiler filter, int limit, int offset, OAuthToken authToken) {
        StringBuilder builder = new StringBuilder(32);
        builder.append(METHOD_URI);
        builder.append("wall.get?");
        builder.append("owner_id=").append(userId).append("&");
        if (limit > 0) {
            builder.append("count=").append(limit).append("&");
        }
        if (offset > 0) {
            builder.append("offset=").append(offset).append("&");
        }
        builder.append("filter=").append(filter.filterName()).append("&");
        builder.append("access_token=").append(authToken.getAccessToken());
        return builder.toString();
    }

    public String wallMessages(String domain, WallFiler filter, int limit, int offset, OAuthToken authToken) {
        StringBuilder builder = new StringBuilder(32);
        builder.append(METHOD_URI);
        builder.append("wall.get?");
        builder.append("domain=").append(domain).append("&");
        if (limit > 0) {
            builder.append("count=").append(limit).append("&");
        }
        if (offset > 0) {
            builder.append("offset=").append(offset).append("&");
        }
        builder.append("filter=").append(filter.filterName()).append("&");
        builder.append("access_token=").append(authToken.getAccessToken());
        return builder.toString();
    }

    public String mutualFriends(int user1Id, int user2Id, OAuthToken authToken) {
        return METHOD_URI + "friends.getMutual?" + 
                "target_uid=" + user2Id + "&" +
                "source_uid" + user1Id + "&" +
                "access_token=" + authToken.getAccessToken();
    }

    public String groupInfo(long groupId, OAuthToken authToken) {
        return METHOD_URI + "groups.getById?" + 
                "gid=" + groupId + "&" +
                "fields=" + "members_count" + "&" +
                "access_token=" + authToken.getAccessToken();
    }
    public String groupInfo(String groupId, OAuthToken authToken) {
        return METHOD_URI + "groups.getById?" +
                "group_id=" + groupId + "&" +
                "fields=" + "members_count" + "&" +
                "access_token=" + authToken.getAccessToken();
    }

    public String groupUsers(long groupId, int count, int offset, OAuthToken authToken) {
        return METHOD_URI + "groups.getMembers?" + 
                "gid=" + groupId + "&" + 
                "count=" + count + "&" +
                "offset=" + offset + "&" + 
                "access_token=" + authToken.getAccessToken();
    }

    public String groupList(long userId,int count,int offset, OAuthToken authToken) {
        return METHOD_URI+"groups.get?"+
                "user_id="+userId+"&"+
                "count=" + count + "&" +
                "offset=" + offset + "&" +
                "fields=members_count&"+
                "extended=1&"+
                "access_token=" + authToken.getAccessToken();
    }

    public String groupList(long userId, OAuthToken authToken) {
        return groupList(userId,1000,0,authToken);
    }
}
