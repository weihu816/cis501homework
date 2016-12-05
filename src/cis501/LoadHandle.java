package cis501;

public interface LoadHandle {

    void setAddress(long addr);
    void addForwardFrom(long addr, long fromAddr);
    void done();

    int getSize();
    long getAddress();
    boolean containAddress(long addr);
    long getLatestForwardFrom(long addr);
    long getBday();
    boolean isDone();
}
