package ru.azaz.vkGetter;

public class Tuple implements Comparable<Tuple> {
    public String val1;
    public Double val2;

    public Tuple(String val1, Double val2) {
        this.val1 = val1;
        this.val2 = val2;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("Tuple{");
        sb.append("val1=").append(val1);
        sb.append(", val2=").append(String.format("%3.3f", val2 * 100)).append(" %");
        sb.append('}');
        return sb.toString();
    }

    @Override
    public int compareTo(Tuple o) {
        if (this.val2 > o.val2)
            return 1;
        if (this.val2 < o.val2)
            return -1;            // Neither val is NaN, thisVal is larger
        return 1;
        //return Double.compare(o.val2, this.val2);
    }
}
