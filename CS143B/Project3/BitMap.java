import java.util.*;

class BitMap{
    int[] map = null;
    int[] mask = null;
    int FRAME_SIZE = 512;

    public BitMap(){
        map =  new int[32];
        for(int i=0;i<map.length;i++){
            map[i] = 0;
        }
        mask =  new int[32];
        for(int i=0;i<map.length;i++){
            mask[i] = 0;
        }
        mask[0] = 0x80000000;
        for(int i=1;i<mask.length;i++){
            mask[i] = mask[i-1]>>>1;
        }
    }

    public void reset(){
        for(int i=0;i<map.length;i++){
            map[i] = 0;
        }
    }

    /**
     * find the position at which there are consecutive 0s in the bitmap
     * @param  num number of 0s to find
     * @return     the position of the find, -1 if not found
     */
    public int search(int num){
        if(num<=0)  return -1;
        int count = 0;
        for (int i=0; i<map.length; i++) {
            for (int j=0; j<mask.length; j++) {
                if ((map[i] & mask[j]) == 0) {
                    count++;
                    if (count == num) {
                        return (i*32 + j - count+1)*FRAME_SIZE;
                    }
                }else{
                    count = 0;
                }
            }
        }
        return -1;
    }


    /**
     * set the bit at position to 1
     * @param pos the position to be set
     */  
    public void set(int addr){
        
        int pos = addr / FRAME_SIZE;
        int re = pos/32;
        map[re] = map[re] | mask[pos%32];
    }

    //reset the bit at position to 0
    public void reset(int addr){
        
        int pos = addr / FRAME_SIZE;
        int re = pos/32;
        map[re] = map[re] & (~mask[pos%32]);
    }
}
