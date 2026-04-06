package org.wx.core.wxBase.unit;

import cn.hutool.core.convert.Convert;
import lombok.SneakyThrows;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author 无心
 * @date 2021/10/18
 * @msg 备注
 * @demo ListUnit
 */
public class ListUnit {
    public static ArrayList<String> toArr(String src){
        return toArr(src,",");
    }
    public static ArrayList<String> toArr(String src,String key){
        if(src==null)return new ArrayList<>();
        if (src.isEmpty())return new ArrayList<>();
        String[] strings = src.contains(key) ? src.split(key) : new String[]{src};
        return  new ArrayList<String>(Arrays.asList(strings));
    }



    @SneakyThrows
    public static <T,E> List<T> getKeyList(Class<T> keyClass, String key,List<E> entityList){
        if (entityList.size()==0)return new ArrayList<T>();
        ArrayList<T> backList = new ArrayList<>();
        Method method = entityList.get(0).getClass().getDeclaredMethod(String.format("get%s",upperFirstCase(key)));
        entityList.forEach(item->{
            try {
                T t = (T) method.invoke(item);
                backList.add(t);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        });
       return backList;
    }
    @SneakyThrows
    public static <T,E> Map<T,E> getKeyMap(Class<T> tClass, Class<E> entityClass, String key, List<E> entityList){
        HashMap<T, E> teHashMap = new HashMap<>();
        Method method = entityClass.getDeclaredMethod(String.format("get%s",upperFirstCase(key)));
        entityList.forEach(item->{
            try {
                T t = (T) method.invoke(item);
                teHashMap.put(Convert.convert(tClass, t),item);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }

        });
        return teHashMap;
    }

    @SneakyThrows
    public static <T,E> List<T> setKey(List<T> entityList,String key,Class<E> valClass,E val){
        if (entityList.size()<=0)return entityList;
        Method method = entityList.get(0).getClass().getDeclaredMethod(String.format("set%s",upperFirstCase(key)),String.class);
        entityList.forEach(item->{
            try {
                method.invoke(item,val);
                //System.err.println(item);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        });
        return entityList;
    }
    public static <T>Boolean isHave(T actionKey,T... actionVal){
        return Arrays.asList(actionVal).contains(actionKey);
    }

    public static ArrayList<String> daySection(String startDate, String endDate) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd");
        ArrayList<String> strings = new ArrayList<>();
        try {
            Calendar editStartPeriod = Calendar.getInstance();
            editStartPeriod.setTime(format.parse(startDate));
            Calendar editEndPeriod = Calendar.getInstance();
            editEndPeriod.setTime(format.parse(endDate));
            editEndPeriod.add(Calendar.DAY_OF_MONTH,1);
            Calendar curr = editStartPeriod;
            while (curr.before(editEndPeriod)) {
                strings.add(dayFormat.format(curr.getTime()));
                curr.add(Calendar.DATE, 1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return strings;
    }

    public static String upperFirstCase(String str) {
        char[] chars = str.toCharArray();
        //首字母小写方法，大写会变成小写，如果小写首字母会消失
        chars[0] -= 32;
        return String.valueOf(chars);
    }
}
