package org.lechisoft.minifw.mybaits;

import java.lang.reflect.Field;

import org.lechisoft.minifw.log.MiniLog;

public class ReflectUtil {

    /**
     * 获取指定对象里面的指定属性对象
     * 
     * @param obj
     *            目标对象
     * @param fieldName
     *            指定属性名称
     * @return 属性对象
     */
    private static Field getField(Object obj, String fieldName) {
        Field field = null;
        for (Class<?> clazz = obj.getClass(); clazz != Object.class; clazz = clazz.getSuperclass()) {
            try {
                field = clazz.getDeclaredField(fieldName);
                break;
            } catch (NoSuchFieldException e) {
                MiniLog.error("no such field [" + fieldName + "]");
            }
        }
        return field;
    }

    /**
     * 获取指定对象的指定属性
     * 
     * @param obj
     *            指定对象
     * @param fieldName
     *            指定属性名称
     * @return 指定属性
     */
    public static Object getFieldValue(Object obj, String fieldName) {
        Object result = null;
        Field field = ReflectUtil.getField(obj, fieldName);
        if (field != null) {
            field.setAccessible(true);
            try {
                result = field.get(obj);
            } catch (IllegalArgumentException e) {
                MiniLog.error("", e);
            } catch (IllegalAccessException e) {
                MiniLog.error("", e);
            }
        }
        return result;
    }

    /**
     * 设置指定对象的指定属性值
     * 
     * @param obj
     *            指定对象
     * @param fieldName
     *            指定属性
     * @param fieldValue
     *            指定属性值
     */
    public static void setFieldValue(Object obj, String fieldName, String fieldValue) {
        Field field = ReflectUtil.getField(obj, fieldName);
        if (field != null) {
            try {
                field.setAccessible(true);
                field.set(obj, fieldValue);
            } catch (IllegalArgumentException e) {
                MiniLog.error("", e);
            } catch (IllegalAccessException e) {
                MiniLog.error("", e);
            }
        }
    }
}