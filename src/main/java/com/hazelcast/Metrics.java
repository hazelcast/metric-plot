package com.hazelcast;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Metrics {

    public static void main(String[] args) throws IOException {
        File file = new File(args[1]);

        if (!file.exists()) {
            throw new IOException("Can't find file " + file);
        }

        Map<String, StringBuffer> result = generate(file);

        StringBuffer data = new StringBuffer();
        StringBuffer select = new StringBuffer();
        StringBuffer selectHandler = new StringBuffer();

        List<String> keys = new ArrayList<>(result.size());
        Map<String,StringBuffer> sanitizedResult = new HashMap<>(result.size());
        Map<String,String> keyMapping = new HashMap<>();
        for(String key: result.keySet()){
            String sanitizedKey = sanitize(key);
            keyMapping.put(sanitizedKey,key);
            keys.add(sanitizedKey);
            sanitizedResult.put(sanitizedKey, result.remove(key));
        }

        result = sanitizedResult;

        Collections.sort(keys);

        for (String name: keys) {

            data.append("var data_"+ name +" = google.visualization.arrayToDataTable([\n");
            data.append("['X', 'Y'],\n");
            data.append(result.get(name));
            data.append("]);\n");

            select.append("   <option value=\"data_"+name+"\">"+escapeHTML(keyMapping.get(name))+"</option>\n");

            selectHandler.append("          if(selValue=='data_"+name+"'){x=data_"+name+";}\n");
        }

        writeResult(data,select,selectHandler);

        // write your code here
    }


    public static String escapeHTML(String s) {
        StringBuilder out = new StringBuilder(Math.max(16, s.length()));
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c > 127 || c == '"' || c == '<' || c == '>' || c == '&') {
                out.append("&#");
                out.append((int) c);
                out.append(';');
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    private static String sanitize(String name) {
        return name
                .replace("/","_")
                .replace(">","_")
                .replace(".","_")
                .replace("[","_")
                .replace("]","_")
                .replace("-","_")
                .replace(":","_");
    }

    private static void writeResult(StringBuffer data,StringBuffer select,StringBuffer selectHandler) throws IOException {
        String template = loadTemplate();
        String result = template.replace("$data", data);
        result = result.replace("$select", select);
        result = result.replace("$handler",selectHandler);
        try (PrintStream out = new PrintStream(new FileOutputStream("result.html"))) {
            out.print(result);
        }
    }

    private static Map<String, StringBuffer> generate(File file) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line = reader.readLine();

        Map<String, StringBuffer> map = new ConcurrentHashMap<>();

        while (line != null) {
            if (line.contains("Metric")) {



//                if (line.contains(metric)) {
//                    lineCount++;
//                    if(lineCount>5){
//                        break;
//                    }
                String[] split = line.split(" ");
                String time = split[2];

                String[] metricKeyValue = split[3].split("=");
                String metric = metricKeyValue[0].substring(7);
                String value = metricKeyValue[1];

                StringBuffer sb = map.computeIfAbsent(metric, k -> new StringBuffer());

                value = value.substring(0, value.length() - 1);
                System.out.println(metric);

                //if(sb.length()<100) {

                    if (sb.length() > 0) {
                        sb.append(",");
                    }

                    sb.append("[new Date(").append(time).append("),").append(value).append("]");
                    sb.append("\n");
                //}
            }
            line = reader.readLine();
        }
        return map;
    }

    private static String loadTemplate() throws IOException {
        ClassLoader classLoader = Metrics.class.getClassLoader();
        File file = new File(classLoader.getResource("template.html").getFile());
        byte[] encoded = Files.readAllBytes(file.toPath());
        return new String(encoded, StandardCharsets.UTF_8);
    }
}
