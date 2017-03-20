import java.util.*;
import java.io.*;
import java.lang.*; 

class Project3{
    //physical memory
    int[] PM = null;
    BitMap bitmap = null;
    //mask and offset for s
    int smask = 0x0ff80000;
    int soffset = 19;
    //mask and offset for p
    int pmask = 0x0007FE00;
    int poffset = 9;
    //mask for w offset
    int wmask = 0x000001ff;
    //output buffer
    StringBuilder sb = null;
    int FRAME_SIZE = 512;
    int HEX_MASK = 0xffffffff;
    TLB tlb = null;
    //first lines of the first test file
    String[] flines = null;
    //second line of the first test file
    String[] slines = null;
    //first line for the second test file
    String[] ilines = null;

    public static void main(String[] args){
        if (args.length <2 | args.length > 2) {
            System.out.println("plz provide proper arguments");
            return;
        }
        try{
            Project3 pro = new Project3();
            pro.initialiseLines(args[0],args[1]);
            pro.reset();
            pro.parse();
            
            pro.reset();
            pro.parseWithTLB();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public Project3(){
        bitmap = new BitMap();
        PM = new int[524288];
        for (int i=0; i<PM.length; i++) {
            PM[i] = 0;
        }
        bitmap.set(0);
        sb = new StringBuilder();
        tlb = new TLB();
    }

    public void reset(){
        bitmap.reset();
        for (int i=0; i<PM.length; i++) {
            PM[i] = 0;
        }
        bitmap.set(0);
        initialise();
        sb = new StringBuilder();
    }

    /**
     * store the files content in flines, slines and ilines
     * @param filename  first input filename
     * @param filename2 second input filename
     */
    public void initialiseLines(String filename,String filename2){
        BufferedReader br = null;
        try{
            br = new BufferedReader(new FileReader(filename));
            flines = br.readLine().split(" ");
            slines = br.readLine().split(" ");
            br.close();
            br = new BufferedReader(new FileReader(filename2));
            ilines = br.readLine().split(" ");
            br.close();
        }catch(FileNotFoundException e){
            System.out.println("file not found");
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    /**
     * initialise segmen table and pagetable using flines, slines and ilines read from the input files
     */
    public void initialise(){
        for (int i=0; i<flines.length; i+=2) {
            int ptaddr = Integer.parseInt(flines[i+1]);
            int saddr = Integer.parseInt(flines[i]);
            System.out.println("segment "+saddr+" pageTableIndex "+ptaddr);
            PM[saddr] = ptaddr;
            //new page table with 2 frames
            bitmap.set(ptaddr);
            bitmap.set(ptaddr+FRAME_SIZE);
        }
        for (int i=0; i<slines.length; i+=3) {    
            int paddr = Integer.parseInt(slines[i]);
            int saddr = Integer.parseInt(slines[i+1]);
            int val = Integer.parseInt(slines[i+2]);
            System.out.println("segment "+saddr+" at page table "+paddr+" val "+val);
            //new page
            PM[PM[saddr]+paddr] = val;
            bitmap.set(val);
        }
    }

    /**
     * parse the second test files input, and generate corresponding result
     * Initially Segment table situated at frame 0, from 0 to 511 words
     * below is the illustration for a virtual address
     * 0000 1111 1111 1222 2222 2223 3333 3333
     * 0s are unused bits, 1s are the offset for segment table, we can obtain the corresponding page table with PM[s]
     * 2s are the offset for pagetable, p, we can get corresponding page with PM[PM[s]+p]
     * 3s are the offset for page PM[PM[PM[s]+p]+w] is the actual physical address
     */
    public void parse(){
        try{
            for (int i=0; i<ilines.length; i+=2) {
                try{
                    int vaddr = Integer.parseInt(ilines[i+1]);

                    int segIndex = ((vaddr & smask) >>> soffset) & HEX_MASK;
                    int pageTableIndex = ((vaddr & pmask) >>> poffset) & HEX_MASK;
                    int w = (vaddr & wmask) & HEX_MASK;

                    System.out.println(Integer.toHexString(vaddr));
                    System.out.println(pageTableIndex+" "+segIndex+" "+w);

                    int val = Integer.parseInt(ilines[i+1]);
                    if (Integer.parseInt(ilines[i])==0) {
                        read(val);
                    }else{
                        write(val);
                    }
                }catch(VException e){
                    continue;
                }
            }
            PrintWriter pw = new PrintWriter(new File("10521466-notlb.txt"));
            pw.print(sb.toString());
            pw.close();

        }catch(Exception e){
            e.printStackTrace();
        }
        
        
    }

    /**
     * parse with TLB
     * the parser first look at TLB see if it is cached in TLB
     * if not, the normal process is continued, and if a new valid physical address is generated,
     * it is added to the TLB
     */
    public void parseWithTLB(){
        try{
            for (int i=0; i<ilines.length; i+=2) {
                try{
                    int vaddr = Integer.parseInt(ilines[i+1]);

                    int segIndex = ((vaddr & smask) >>> soffset) & HEX_MASK;
                    int pageTableIndex = ((vaddr & pmask) >>> poffset) & HEX_MASK;
                    int w = (vaddr & wmask) & HEX_MASK;
                    
                    System.out.println(Integer.toHexString(vaddr));
                    System.out.println(pageTableIndex+" "+segIndex+" "+w);

                    int val = Integer.parseInt(ilines[i+1]);

                    int paddr = 0;
                    if ((paddr = tlb.lookup(val)) != -1) {
                        //if it is in the tlb
                        sb.append("h "+Integer.toString(paddr+w)+" ");
                        continue;
                    }else{
                        sb.append("m ");
                    }

                    if (Integer.parseInt(ilines[i])==0) {
                        paddr = read(val);
                    }else{
                        paddr = write(val);
                    }
                    tlb.newEntry(val,paddr);
                }catch(VException e){
                    continue;
                }
            }
            PrintWriter pw = new PrintWriter(new File("10521466-tlb.txt"));
            pw.print(sb.toString());
            pw.close();

        }catch(Exception e){
            e.printStackTrace();
        }
    }

    //page fault
    private void pf() throws Exception{
        sb.append("pf ");
        throw new VException("pf");
    }
    //error in reading virtual memory
    private void er() throws Exception{
        sb.append("err ");
        throw new VException("error");
    }
    //output the resulting physical address
    private void output(int val) throws Exception{
        sb.append(Integer.toString(val)+" ");
    }

    /**
     * reading a virtual address, if the page table is empty, error(). or if there is a page fault, throw pagefault Exception
     * @param  vaddr     virtual address
     * @return           page starting address
     * @throws Exception virtual address lookup exception
     */
    public int read(int vaddr) throws Exception{
        int segIndex = ((vaddr & smask) >>> soffset) & HEX_MASK;
        int pageTableIndex = ((vaddr & pmask) >>> poffset) & HEX_MASK;

        int pageAddr = -1;
        if (segIndex >= FRAME_SIZE || PM[segIndex] == 0) {
            er();
        }else if(PM[segIndex] == -1){
            pf();
        }else{
            pageAddr = PM[PM[segIndex]+pageTableIndex];
            if(pageAddr == 0)    er();
            else if(pageAddr == -1)  pf();

            int w = (vaddr & wmask) & HEX_MASK;
            output(pageAddr+w);
        }
        return pageAddr;
    }

    /**
     * writing a value to a virtual address. if the corresponding pagetable or page is nonexistent
     * new page table or page is created.
     * @param  vaddr     virtual address
     * @return           page starting address
     * @throws Exception page fault exception
     */
    public int write(int vaddr) throws Exception{
        int segIndex = ((vaddr & smask) >>> soffset) & HEX_MASK;
        

        if (segIndex >= FRAME_SIZE) {
            er();
        }else if(PM[segIndex] == -1){
            pf();
        }else if(PM[segIndex] == 0){
            int address = bitmap.search(2);
            System.out.println("new PT addr "+address);
            PM[segIndex] = address;
            bitmap.set(address);
            bitmap.set(address+FRAME_SIZE);
        }

        int pageTableIndex = ((vaddr & pmask) >>> poffset) & HEX_MASK;


        int pageAddr = PM[PM[segIndex]+pageTableIndex];
        if (pageAddr == -1) {
            pf();
        }else if(pageAddr == 0){
            int address = bitmap.search(1);
            PM[PM[segIndex]+pageTableIndex] = address;
            System.out.println("new page addr "+address);
            bitmap.set(address);
        }

        int w = (vaddr & wmask) & HEX_MASK;
        output(PM[PM[segIndex]+pageTableIndex]+w);
        return PM[PM[segIndex]+pageTableIndex];
    }

    /**
     * custom exception for virtual address translation
     */
    public class VException extends Exception {
        public VException(String message) {
            super(message);
        }
    }
}
