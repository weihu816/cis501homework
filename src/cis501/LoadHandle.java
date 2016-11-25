package cis501;

public interface LoadHandle {

    void setAddress(long addr);
    void addForwardFrom(long addr);
    void done();

    int getSize();
    long getAddress();
    boolean containAddress(long addr);
    long getLatestForwardFrom();
    long getBday();
    boolean isDone();
}
