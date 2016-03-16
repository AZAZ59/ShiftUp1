package ru.azaz.vkGetter;

import com.googlecode.vkapi.HttpVkApi;
import com.googlecode.vkapi.domain.VkOAuthToken;
import com.googlecode.vkapi.domain.group.VkGroup;
import com.googlecode.vkapi.exceptions.VkException;
import ru.azaz.ratioFunctions.ShiftUp;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


public class Main {
    public static void main(final String[] args) throws Exception {
        HttpVkApi vkApi = new HttpVkApi("5303841", "WmX01fbneO4o20eONdF4", "https://oauth.vk.com/blank.html");
        System.out.println(vkApi.getAuthUri());
        VkOAuthToken tok = new VkOAuthToken("00daf086832d7ed7230e88934936d4470aec582bfecf7cf7ec7c5040510fd4386c3a01e3ee3fc19169644", 0, 28499999);

        String group1 = "jablog";

        //ShiftUp sf = new ShiftUp(vkApi, tok);
        //LexAnalyzer lex = new LexAnalyzer(vkApi, tok);


        /*Future<TreeSet<Tuple<String, Double>>> lexMainGroup = lex.asyncGetAndAnalize(group1, 500);
        Set<TreeSet<Tuple<String, Double>>> lexOtherGroups = Collections.synchronizedSet(new HashSet<>());
        ExecutorService ess = Executors.newFixedThreadPool(20);

        Tuple<Map<VkGroup, Integer>, Integer> data = sf.numOfIntersectGroup(group1, 100);

        Map<VkGroup, Integer> map = data.val1;
        int count = data.val2;

        Tuple<VkGroup, Double>[] arr = new Tuple[map.size()];
        final int[] i = {0};
        map.forEach((group, integer) -> arr[i[0]++] = new Tuple(group, integer + .0));
        Arrays.sort(arr);
        Arrays.stream(arr).filter(t -> t.val1.getMembersCount() != 0).limit(20).filter(t -> (t.val2 / count > 0.01)).forEach(
                t -> {
                    System.out.printf("%-50s    %-10d   %02.5f %% \t %02.5f %% \t %d\n",
                            t.val1.getGroupName().trim(),
                            t.val1.getGroupId(),
                            (t.val2 / t.val1.getMembersCount() * 100),
                            (t.val2 / count * 100),
                            t.val2.intValue()
                    );
                    ess.submit(() -> lexOtherGroups.add(lex.getAndAnalize(t.val1.getGroupId(), 500)));
                }
        );
        ess.shutdown();
        ess.awaitTermination(1, TimeUnit.DAYS);

        TreeSet<Tuple<String, Double>> mainLex = lexMainGroup.get();
        Map<String, Double> mainLexMap;
        mainLexMap = mainLex.stream().collect(Collectors.toMap(stringDoubleTuple1 -> stringDoubleTuple1.val1, stringDoubleTuple2 -> stringDoubleTuple2.val2));
        int max = 0;
        TreeSet<Tuple<String, Double>> maxIntersect = null;
        Double maxGrid = 0.;

        ArrayList<Tuple<TreeSet<Tuple<String, Double>>,Integer>> intersects=new ArrayList<>();

        for (TreeSet<Tuple<String, Double>> tuples : lexOtherGroups) {
            TreeSet<Tuple<String, Double>> curIntersect =
                    fullIntersect(
                            tuples.stream()
                                    .collect(
                                            Collectors.toMap(
                                                    stringDoubleTuple -> stringDoubleTuple.val1,
                                                    stringDoubleTuple -> stringDoubleTuple.val2
                                            )
                                    )
                            , mainLexMap
                    );
            intersects.add(new Tuple<>(curIntersect,curIntersect.size()));
            if (curIntersect.size() > max) {
                maxIntersect = curIntersect;
                max = curIntersect.size();
                maxGrid = tuples.stream().filter(stringDoubleTuple -> stringDoubleTuple.val1.equals("*-*-*-*-*-*-*-*-*-*-*-*-")).findFirst().get().val2;
            }
        }

        Collections.sort(intersects);


        System.out.println("maxIntersect = " + maxIntersect);
        System.out.println("max =" + max);
        System.out.println("maxGrid=" + maxGrid);
        System.out.println("=================================");
        intersects.forEach(t->{
            System.out.println(t.val1);
            System.out.println(t.val2);
            System.out.println("=================================");
        });

        return;
        /*analyzeAllGroupOfPeople(id, vkApi, tok);
        analyzeAllGroupOfPeople(59183759, vkApi, tok);

        //29627241
        //30666517
        //20629724 хабр
        //31969346  СТ

        System.exit(0);
        TreeSet<Tuple<String,Double>> set2 = getAndAnalize("ssau_v_teme", vkApi, tok);
        set2.stream().
                filter(tuple2 -> tuple2.val2.doubleValue() * 100 > 0.5).
                sorted((t1, t2) -> Double.compare(t1.val2.doubleValue(), t2.val2.doubleValue())).
                forEach(tuple1 -> System.out.println(tuple1));


        TreeSet<Tuple<String,Double>> set1 = getAndAnalize("ssau_v_teme", vkApi, tok);


        HashMap<String, Double> map1 = new HashMap<>();
        set1.forEach(tuple -> map1.put(tuple.val1, tuple.val2));

        HashMap<String, Double> map2 = new HashMap<>();
        set2.forEach(tuple -> map2.put(tuple.val1, tuple.val2));

        TreeSet<Tuple<String,Double>> rs = new TreeSet<>();


        fullIntersect(map1, map2, rs);
        fullIntersect(map2, map1, rs);


        PrintWriter pw2 = new PrintWriter(new File("output_" + "123eed" + ".txt"));


        System.out.println("+===============================");
        rs.forEach(s -> {
            if (s.val2 * 100 >= 0.5) System.out.println(s);
        });
        System.out.println("+===============================");
        System.out.println(rs.parallelStream().collect(Collectors.averagingDouble(tuple -> tuple.val2 * 100)));
        pw2.close();*/
    }

    private static void analyzeAllGroupOfPeople(long id, HttpVkApi vkApi, VkOAuthToken tok) throws VkException {
        /*Collection<VkGroup> list = vkApi.groupsOfUser(id, tok);
        ExecutorService es = Executors.newFixedThreadPool(20);
        System.out.println(list);
        final int[] i = {0};
        list.stream().forEach(vkGroup -> /*es.submit((Runnable) () -> {
          /*          try {
                        System.out.println("Start analizing group " + vkGroup.getGroupName() + " " + (i[0]++) + " from " + list.size());

                        File f = new File("allGroups/" + id + "_" + vkGroup.getGroupName() + ".txt");
                        if (!f.exists()) f.createNewFile();
                        else if (f.length() == 0) {
                            PrintWriter pw = new PrintWriter(f);
                            TreeSet<Tuple<String,Double>> set = getAndAnalize(vkGroup.getGroupId(), vkApi, tok, 20000);
                            set.forEach(tuple -> pw.println(tuple.val1 + " " + (tuple.val2 * 100) + "%"));
                            pw.close();

                        } else {
                            System.err.println("                    " + f + " skipped");
                        }

                        System.out.println("End analizing group " + vkGroup.getGroupName());
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (VkException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
        );//);
        /*es.shutdown();
        try {
            es.awaitTermination(222, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/

    }

    private static TreeSet<Tuple<String, Double>> fullIntersect(Map<String, Double> map1, Map<String, Double> map2) {
        TreeSet<Tuple<String, Double>> rs = new TreeSet<>();
        map1.forEach((s, aDouble) -> {
            if (map2.containsKey(s)) {
                rs.add(new Tuple(s, (map2.get(s) + aDouble) / 2));
            }
        });
        map2.forEach((s, aDouble) -> {
            if (map1.containsKey(s)) {
                rs.add(new Tuple(s, (map1.get(s) + aDouble) / 2));
            }
        });
        return rs;
    }


}

