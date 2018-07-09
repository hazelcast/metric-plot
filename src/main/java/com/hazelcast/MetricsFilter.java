package com.hazelcast;

import com.yevdo.jwildcard.JWildcard;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MetricsFilter implements Predicate<String>{

    private List<Pattern> patterns = new LinkedList<>();

    public MetricsFilter(String filters){

        for(String filter: filters.split(",")){
            String regexString = JWildcard.wildcardToRegex(filter);
            Pattern pattern = Pattern.compile(regexString);
          patterns.add(pattern);
        }


    }

    @Override
    public boolean test(String s) {
       for(Pattern pattern: patterns){
           if(pattern.matcher(s).matches()){
               return true;
           }
       }

       return false;
    }
}
