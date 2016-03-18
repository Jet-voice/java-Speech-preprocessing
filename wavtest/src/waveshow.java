import java.io.FileWriter;


public class waveshow {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
//		 String filename = "d://a191.wav";  
//         Vad vad = new Vad(filename);
	  //      WaveFileReader reader = new WaveFileReader(filename); 
	 //       int max = 0;
	//        System.out.println("²ÉÑùÂÊ"+reader.getSampleRate());//11025
	         //   System.out.println(max);//55296
//	            try{
//	            	FileWriter fileWriter=new FileWriter("d:\\javaresult.txt");
//	            for(int i=0;i<vad.FrmNum;i++){
//	            	for(int j=0;j<vad.FRM_LEN;j++){
//	            	double a = vad.audFrame[i].fltFrame[j];
//	             fileWriter.write(String.valueOf(a)+" ");
//	            	}
//	            
//	            }fileWriter.flush();
//	            fileWriter.close();
//	            }catch(Exception e){
//	            	
//	            }
//         System.out.println(vad.WavStart); 
//	           System.out.println(vad.WavEnd); 
		MFCC mfcc = new MFCC();
		mfcc.getMfcc("d://a191.wav");
	        }    
	    }  
	


