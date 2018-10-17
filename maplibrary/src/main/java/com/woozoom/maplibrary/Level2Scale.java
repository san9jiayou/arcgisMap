package com.woozoom.maplibrary;

/*
     2.103675414420051E8        1
     1.0518377072100255E8       2
     5.2591885360501274E7       3
     2.6295942680250637E7       4
     1.3147971340125319E7       5
     6573985.670062659          6
     3286992.8350313297         7
     1643496.4175156648         8
     821748.2087578324          9
     410874.1043789162          10
     205437.0521894581          11
     102718.52609472905         12
     51359.263047364526         13
     25679.631523682263         14
     12839.815761841131         15
     6419.907880920566          16
     3209.953940460283          17
     1677.8866464440896         18
     838.9433232220448          19
     */

/**
 * 级别及对应zoom
 */
public enum Level2Scale {
    ONE(1, 2.103675414420051E8),
    TWO(2, 1.0518377072100255E8),
    THREE(3, 5.2591885360501274E7),
    FOUR(4, 2.6295942680250637E7),
    FIVE(5, 1.3147971340125319E7),
    SIX(6, 6573985.670062659),
    SEVEN(7, 3286992.8350313297),
    EIGHT(8, 1643496.4175156648),
    NINE(9, 821748.2087578324),
    TEN(10, 410874.1043789162),
    ELEVEN(11, 205437.0521894581),
    TWELVE(12, 102718.52609472905),
    THIRTEEN(13, 51359.263047364526),
    FOURTEEN(14, 25679.631523682263),
    FIFTEEN(15, 12839.815761841131),
    SIXTEEN(16, 6419.907880920566),
    SEVENTEEN(17, 3209.953940460283),
    EIGHTEEN(18, 1677.8866464440896),
    NINETEEN(19, 838.9433232220448),;


    private final int key;
    private final double value;

    public int getKey() {
        return key;
    }

    public double getValue() {
        return value;
    }

    Level2Scale(int key, double value) {
        this.key = key;
        this.value = value;
    }

    /**
     * 根据key获取value
     *
     * @param key : 键值key
     * @return String
     */
    public static double getValueByKey(int key) {
        Level2Scale[] enums = Level2Scale.values();
        for (int i = 0; i < enums.length; i++) {
            if (enums[i].getKey() == key) {
                return enums[i].getValue();
            }
        }
        return 1677.8866464440896;
    }
}