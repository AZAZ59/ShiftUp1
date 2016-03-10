package ru.azaz.vkGetter;

import com.googlecode.vkapi.HttpVkApi;
import com.googlecode.vkapi.domain.VkOAuthToken;
import com.googlecode.vkapi.domain.group.VkGroup;
import com.googlecode.vkapi.domain.user.VkUser;
import com.googlecode.vkapi.exceptions.VkException;

import java.io.*;
import java.util.*;

public class Analizer {
    private static TreeSet<Tuple> analize(File f1) throws IOException {

        TreeSet<Tuple> set = new TreeSet<>();

        BufferedReader br1 = new BufferedReader(new FileReader(f1));

        HashMap<String, Double> map = new HashMap<>();
        HashSet<String> hs = new HashSet<>();
        String s = "";
        long wordCount = 0;
        while ((s = br1.readLine()) != null) {
            s += "|";
            try {
                String key = s.substring(0, s.indexOf(':'));
                double val = java.lang.Double.parseDouble(s.substring(s.indexOf(":") + 1, s.indexOf("=")));
                String sp = s.substring(s.indexOf("=") + 1);
                String type = sp.substring(0, sp.indexOf("="));
                try {
                    type = type.substring(0, type.indexOf(","));
                } catch (Exception e) {
                    ;
                }

                if (/*type.equalsIgnoreCase("V") ||*/ type.equalsIgnoreCase("A") || type.equalsIgnoreCase("S")) {
                    if (!map.containsKey(key)) {
                        map.put(key, 0d);
                    }
                    map.put(key, map.get(key) + 1);
                    wordCount++;
                }
            } catch (Exception e) {
                //System.err.println(s);
            }
        }

        final long finalWordCount = wordCount;
        map.entrySet().forEach(stringDoubleEntry -> set.add(new Tuple(stringDoubleEntry.getKey(), stringDoubleEntry.getValue() / finalWordCount)));
        /*tmp.forEach(s1 -> {
            System.out.println(s1);
        });*/

        return set;


    }

    private static void writeTransponed(HttpVkApi vkApi, VkOAuthToken tok, VkUser user, ArrayList<VkUser> users, HashMap<VkGroup, HashSet<VkUser>> data) throws FileNotFoundException, VkException {
        PrintWriter pw2 = new PrintWriter(new File("transponed.csv"));

        ArrayList<VkUser> names = new ArrayList<>();
        users.stream().forEach((arg) -> names.add(arg));//все юзеры

        String nam = Arrays.toString(names.stream().map(
                (arg) -> arg.getFirstName() + " " + arg.getLastName()
        ).toArray());
        nam = "gr_name," + nam.substring(1, nam.length() - 1);
        //заголовок

        pw2.println(nam + ",-123");
        for (VkGroup grp : vkApi.groupsOfUser(user.getVkUserId(), tok)) {
            pw2.print("\"" + grp.getGroupName().replaceAll("\\\"", "`").replaceAll("&...", "`").replaceAll("[^а-яА-Яa-zA-Z0-9\\[\\] ]", "") + "\",");
            for (VkUser u : users) {
                pw2.print((data.get(grp).contains(u) ? "1" : "0") + ",");
            }
            pw2.print("-123");
            pw2.println();
        }

        pw2.flush();
        pw2.close();
    }

    private static void WriteStraight(HttpVkApi vkApi, VkOAuthToken tok, VkUser user, ArrayList<VkUser> users, HashMap<VkGroup, HashSet<VkUser>> data, ArrayList<VkGroup> groups) throws FileNotFoundException, VkException {
        PrintWriter pw = new PrintWriter(new File("straight.csv"));

        Collection<VkGroup> grList = vkApi.groupsOfUser(user.getVkUserId(), tok);

        String gr = Arrays.toString(grList.stream().map(VkGroup::getGroupId).toArray());
        gr = gr.substring(1, gr.length() - 1);//заголовок

        pw.println("name," + gr + ",0");

        for (VkUser u : users) {
            pw.print(u.getFirstName() + " " + u.getLastName() + ",");
            for (VkGroup id : grList) {
                pw.print((data.get(id).contains(u) ? "1" : "0") + ",");
            }
            pw.print('0');
            pw.println();
        }

        pw.flush();
        pw.close();
    }

    private static HashMap<VkGroup, HashSet<VkUser>> GetDataFromVK(HttpVkApi vkApi, VkOAuthToken tok, ArrayList<VkUser> users) throws VkException {
        HashMap<VkGroup, HashSet<VkUser>> data = new HashMap<VkGroup, HashSet<VkUser>>();
        long l = 0;
        for (VkUser u : users) {
            Collection<VkGroup> vkGroups = vkApi.groupsOfUser(u.getVkUserId(), tok);
            System.out.printf("%40s %4d from %4d with %4d groups %n", u.getFirstName() + " " + u.getLastName(), ++l, users.size(), vkGroups.size());
            for (VkGroup g : vkGroups) {
                if (!data.containsKey(g)) {
                    data.put(g, new HashSet<>());
                }
                data.get(g).add(u);
            }
        }
        return data;
    }

}

