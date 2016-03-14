package ru.azaz.vkGetter;

import com.googlecode.vkapi.HttpVkApi;
import com.googlecode.vkapi.WallFiler;
import com.googlecode.vkapi.domain.VkOAuthToken;
import com.googlecode.vkapi.domain.group.VkGroup;
import com.googlecode.vkapi.domain.message.VkWallMessage;
import com.googlecode.vkapi.exceptions.VkException;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import ru.stachek66.nlp.mystem.holding.Factory;
import ru.stachek66.nlp.mystem.holding.MyStem;
import scala.Option;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

/**
 * Created by AZAZ on 14.03.2016.
 */
public class LexAnalyzer {
    HttpVkApi vkApi;
    VkOAuthToken tok;
    int numDownloadThreads =20;
    int numParseThread=4;
    GenericObjectPool<MyStem> pool = new GenericObjectPool<MyStem>(new BasePooledObjectFactory<MyStem>() {
        @Override
        public MyStem create() throws Exception {
            return new Factory("-gid --format json")
                    .newMyStem("3.0", Option.<File>empty()).get();
            //.newMyStem("3.0", Option.<File>apply(new File("C:\\\\Users\\\\AZAZ\\\\Desktop\\\\DL\\\\stammig\\\\mystem.exe"))).get();
        }

        @Override
        public PooledObject<MyStem> wrap(MyStem myStem) {
            return new DefaultPooledObject<>(myStem);
        }
    });


    public LexAnalyzer(HttpVkApi vkApi, VkOAuthToken tok) {
        this(vkApi,tok,20);
    }

    public LexAnalyzer(HttpVkApi vkApi, VkOAuthToken tok, int numDownloadThreads) {
        this(vkApi,tok,numDownloadThreads,4);
    }

    public LexAnalyzer(HttpVkApi vkApi, VkOAuthToken tok, int numDownloadThreads, int numParseThread) {
        this.vkApi = vkApi;
        this.tok = tok;
        this.numDownloadThreads = numDownloadThreads;
        this.numParseThread = numParseThread;

        IntStream.range(0, numParseThread).forEach(value -> {
            try {
                pool.addObject();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public LexAnalyzer() {
    }


    Future<TreeSet<Tuple<String,Double>>> asyncGetAndAnalize(Object grid, int limit){
        FutureTask task= new FutureTask<>(() -> {
            System.out.println("Start "+grid);
            Collection<VkWallMessage> messages = null;
            if (grid instanceof Number) {
                if (grid instanceof Integer) {
                    messages = vkApi.lastGroupWallMessages((Integer) grid, WallFiler.ALL, limit, tok);
                }
                if (grid instanceof Long) {
                    messages = vkApi.lastGroupWallMessages((Long) grid, WallFiler.ALL, limit, tok);
                }
            }
            if (grid instanceof String) {
                messages = vkApi.lastGroupWallMessages((String) grid, WallFiler.ALL, limit, tok);
            }

            Map<String, Double> tmp = Collections.synchronizedMap(new HashMap<>());

            final StringBuilder[] sb = {new StringBuilder("")};
            ExecutorService es = Executors.newFixedThreadPool(numDownloadThreads);
            AtomicLong wordCount = new AtomicLong(0);
            final int[] i = {0};
            messages.forEach(vkWallMessage -> {
                i[0]++;
                sb[0].append(vkWallMessage.getText());
                if (i[0] % 200 == 0) {
                    es.execute(new Parallels(wordCount, tmp, sb[0].toString(), vkWallMessage.getCommentsCount() + vkWallMessage.getLikesCount() + vkWallMessage.getRepostCount() + 1,pool));
                    sb[0] = new StringBuilder("");
                }
            });
            es.shutdown();
            try {
                System.err.println(es.awaitTermination(48, TimeUnit.HOURS));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            TreeSet<Tuple<String, Double>> set = new TreeSet<>();
            tmp.entrySet().stream().forEach(
                    stringDoubleEntry -> set.add(
                            new Tuple<>(stringDoubleEntry.getKey(), stringDoubleEntry.getValue() / wordCount.get())
                    )
            );
            VkGroup gr = vkApi.groupInfo(grid.toString(),tok);
            set.add(new Tuple<>("*-*-*-*-*-*-*-*-*-*-*-*-",gr.getGroupId()+.0));
            System.out.println("End "+grid);
            return set;
        });
        task.run();
        return task;
    }
    Future<TreeSet<Tuple<String,Double>>> asyncGetAndAnalize(Object grid){
        return asyncGetAndAnalize(grid,0);
    }

    TreeSet<Tuple<String,Double>> getAndAnalize(Object grid, int limit) throws VkException, FileNotFoundException {
        try {
            return asyncGetAndAnalize(grid, limit).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    TreeSet<Tuple<String,Double>> getAndAnalize(Object grid) throws VkException, FileNotFoundException {
        return getAndAnalize(grid, 0);
    }


}
