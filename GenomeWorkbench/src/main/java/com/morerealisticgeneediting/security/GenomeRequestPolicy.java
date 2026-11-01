package com.morerealisticgeneediting.security;
import java.util.UUID;
/** Pure validation rules, shared by server handlers and regression tests. */
public final class GenomeRequestPolicy {
    public static final int MAX_SLICE=4096, MAX_MOTIF=64, MAX_HITS=128, MAX_SCAN=65536, MAX_TEMPLATE=1024;
    private GenomeRequestPolicy() {}
    public static boolean validRange(long start,int length,long total,int limit) {
        return start>=0 && length>0 && length<=limit && start<=total && length<=total-start;
    }
    public static boolean validDna(String value,int limit) {
        return value!=null && !value.isEmpty() && value.length()<=limit && value.matches("[ACGTacgt]+");
    }
    public static boolean matches(UUID request,UUID session,int requestedSync,int actualSync,long requestedRevision,long actualRevision) {
        return request!=null && request.equals(session) && requestedSync==actualSync && requestedRevision>=0 && requestedRevision==actualRevision;
    }
}
