package cis501;

public interface StoreHandle {

    void setValue(long val);
    void setAddress(long addr);
    void done();

    int getSize();
    long getAddress();
    long getByte(long addr);
    boolean containAddress(long addr);
    long getValue();
    long getBday();
    boolean isDone();
}
