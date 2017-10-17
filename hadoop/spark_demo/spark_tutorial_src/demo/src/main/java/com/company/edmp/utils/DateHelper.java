package com.company.edmp.utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by yancai on 2016/7/6.
 */
public class DateHelper {

    /**
     * 获取当前日期
     * @return
     */
    public static String getDate() {
        DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(new Date());
    }
}
