package ru.azaz.vkGetter;

public class Tuple<T,B extends Number> implements Comparable<Tuple> {
    public T val1;
    public B val2;

    public Tuple(T val1, B val2) {
        this.val1 = val1;
        this.val2 = val2;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("Tuple{");
        sb.append("val1=").append(val1);
        sb.append(", val2=").append(String.format("%3.3f", val2.doubleValue() * 100)).append(" %");
        sb.append('}');
        return sb.toString();
    }

    @Override
    public int compareTo(Tuple o) {
        /*if (Double.compare(o.val2.doubleValue(),this.val2.doubleValue())==0){

            return 1;
        }*/
        return Double.compare(o.val2.doubleValue(),this.val2.doubleValue());
    }
}
