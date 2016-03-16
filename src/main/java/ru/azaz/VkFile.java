package ru.azaz;

import com.googlecode.vkapi.HttpVkApi;
import com.googlecode.vkapi.domain.VkOAuthToken;

/**
 * Created by AZAZ on 16.03.2016.
 */
public abstract class VkFile {
    public HttpVkApi vkApi;
    public VkOAuthToken tok;
    public int threadCount=20;

    public VkFile(HttpVkApi vkApi, VkOAuthToken tok,int threadCount) {
        this.vkApi = vkApi;
        this.tok = tok;
        this.threadCount=threadCount;
    }

    public VkFile(HttpVkApi vkApi, VkOAuthToken tok) {
        this.vkApi = vkApi;
        this.tok = tok;
    }

    public VkFile() {
    }
}
