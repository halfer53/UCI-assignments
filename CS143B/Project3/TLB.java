import java.util.*;


class TLB{
    class CacheEntry{
        //ri is the priority of the entry
        //3 indicating most recently used, 0 least recently used
        private int _ri = 0;
        //segment offset and page table offset
        private int _pa = 0;
        //frame number of the corresponding virtual address
        private int _frameNum = 0;
        public CacheEntry(int ri,int pa,int frameNum){
            _ri = ri;
            _pa = pa;
            _frameNum = frameNum;
        }
        public int getRI(){ return _ri;}
        public void setRI(int ri){    _ri = ri;}
        public int getPA(){ return _pa;}
        public void setPA(int pa){  _pa = pa;}
        public int getFrame(){  return _frameNum;}
        public void setFrame(int frameNum){ _frameNum = frameNum;}
        public void decrementRI(){  _ri--;}
    }

    CacheEntry[] tlb = null;
    public TLB(){
        tlb = new CacheEntry[4];
        for (int i=0; i<tlb.length; i++) {
            tlb[i]  = new CacheEntry(-1,0,0);
        }
    }

    /**
     * it first see if there is any enpty cache entry
     * if not, select the least recently used cache
     * @return the pointer to the cache entry
     */
    private int firstVacancy(){
        for (int i=0; i<tlb.length; i++) {
            if (tlb[i].getRI() == -1) {
                return i;
            }
        }
        for (int i=0; i<tlb.length; i++) {
            if (tlb[i].getRI() == 0) {
                return i;
            }
        }
        return -1;
    }

    /**
     * see if the virtual address is in the tlb, if it is, 
     * it's RI is updated as the most recently used, and all others which are greater than it are decremented by 1
     *  
     * @param  vaddr virtual address 
     * @return       the physical address of the parameter, or -1 if its not present in tlb
     */
    public int lookup(int vaddr){
        int pa = (vaddr & 0x0FFFFE00) >>> 9;
        int k = -1;
        for (int i=0; i<tlb.length; i++) {
            if (tlb[i].getPA() == pa && tlb[i].getRI() != -1) {
                k = i;
                break; 
            }
        }
        if (k!=-1) {
            for (int i=0; i<tlb.length; i++) {
                if (tlb[i].getRI() > tlb[k].getRI()) {
                    tlb[i].decrementRI();
                }
            }
            tlb[k].setRI(3);
        }
        return k!= -1 ? tlb[k].getFrame() * 512 : -1;
    }

    /**
     * add a new entry to the tlb. select an empty entry or least recently used entry, update it as most recently used
     * and lower the RI of all other entries
     * @param vaddr virtual address
     * @param f     the corresponding page starting address
     */
    public void newEntry(int vaddr,int f){
        int pa = (vaddr & 0x0FFFFE00) >>> 9;
        int frameNum = f/512;
        int k = firstVacancy();
        if (k!=-1) {
            tlb[k].setRI(3);
            tlb[k].setPA(pa);
            tlb[k].setFrame(frameNum);
            for (int i=0; i<tlb.length; i++) {
                if (tlb[i].getRI() > 0) {
                    tlb[i].decrementRI();
                }
            }
        }
        
    }
}