import java.io.FileWriter;
import java.text.DecimalFormat;



public class Vad {

	public static double PI1 = 3.1415926536;// ����pi
	public static int FRM_LEN = 256;// ����֡��
	public static int FRM_SFT = 80;// ����֡��
	public static double CHANGE = Math.pow(2, 15);
	public int FrmNum;// �ܹ�����֡
	public int dwSoundlen;// ���������
	public int predata[];// ����15�η�������һ��ǰ������
	public double data[];//����15�η�������һ���������
	public double fpData[];// Ԥ���غ�Ĳ�����
	public float fltHamm[];// Hamming��ϵ��
	public AudFrame audFrame[];// ֡����
	public double fltZcrVadThresh;//������ ��ֵ,0.02
	double fltSteThresh[];     //˫���޶�ʱ������ֵ[0]��[1]��   
	double	dwZcrThresh[];  //˫���޹�������ֵ[0]��[1]��   
	int   WavStart;//������ʵ��
	int   WavEnd;//����������
	public static int  MIN_WORD_LEN=15;//��С��������
	public static int  MAX_SLIENCE_LEN=8; 	//���������
	//public static int WORD_MAX_SLIENCE =10;  //���������������
	public double dwWordLen;//�˵���������γ���
	public double maxData;//���Ĳ�����
	public Vad(String filename) {
		 ReadWav(filename);
		// AudPreEmphasize();//Ԥ����
//		 AunEnframe();//��֡
//		 Hamming() ;//��hammingϵ��
//		AudHamming();//�Ӵ�
//		AudSte();//����ʱ����
//		AudZcr();//��������
//		AudNoiseEstimate();	//����������ֵ
//		AudVadEstimate();//�˵���
	}
	
	/***************************
	*MFCC�õĶ˵���
	*������: WaveEndtest(void)
	*���ܣ��˵���
	*************************/
	public void WaveEndtest() 
	{
		AunEnframe();//��֡
		Hamming() ;//��hammingϵ��
		AudHamming();//�Ӵ�
		AudSte();//����ʱ����
		AudZcr();//��������
		AudNoiseEstimate();	//����������ֵ
		AudVadEstimate();//�˵���
	}
	
	

	/***********************************
	 * ��ȡ��Ƶ ��������ReadWav() ���ܣ���ȡ��Ƶ
	 ************************************/
	public void ReadWav(String filename) {
		
		WaveFileReader reader = new WaveFileReader(filename);
		if (reader.isSuccess()) {
			predata = reader.getData()[0]; // ��ȡ��һ����
			dwSoundlen = predata.length;
		} else {
			System.err.println(filename + "����һ��������wav�ļ�");
		}
		data = new double[dwSoundlen];
		for(int i=0;i<dwSoundlen;i++){
			data[i]= predata[i]/CHANGE;
		}
		
		vadCommon();//��һ��
		
		
		
	}

	/***********************************
	 * Ԥ���� ��������AudPreEmphasize() ���ܣ������в��������Ԥ����
	 *  % Ԥ�����˲��� 
  	 * xx=double(x); 
     * xx=filter([1 -0.9375],1,xx); 
	 ************************************/

	public void AudPreEmphasize() {
		fpData = new double[dwSoundlen];
		fpData[0] = data[0];
		for (int i = 1; i < dwSoundlen; i++) {
			fpData[i] = (double) (data[i]) - (double) (data[i - 1]) * 0.9375;
		}


	}

	/***********************************
	 * ��֡ ��������AudEnframe() ���ܣ���ÿһ֡��fltFrame[FRM_LEN]���������ֵ��������֡��
	 * fpData��Ԥ���غ������
	 ************************************/
	public void AunEnframe() {
		FrmNum = (dwSoundlen - (FRM_LEN - FRM_SFT)) / FRM_SFT;
		audFrame = new AudFrame[FrmNum];
		for(int i=0;i<FrmNum;i++){
			audFrame[i] = new AudFrame();
		}
		int x = 0;// ÿһ֡����ʼ��
		for (int i = 0; i < FrmNum; i++) {
			audFrame[i].fltFrame = new double[FRM_LEN];
			
			for (int j = 0; j < FRM_LEN; j++) {
				audFrame[i].fltFrame[j] = data[x + j];
			}
			x+=FRM_SFT;
		}
		
		
	}

	/***********************************
	 * ������ϵ�� ��������Hamming()
	 * ���ܣ�������ϵ�����������ÿһ֡��֡����Ҫ�õ�PI����������ǹ̶�ֵ��ֻ��֡������
	 ************************************/
	public void Hamming() {
		fltHamm = new float[FRM_LEN];
		for (int i = 0; i < FRM_LEN; i++) {
			// ����������ΪW(n,a) = (1-a) -��cos(2*PI*n/(N-1))
			// 0�Qn�QN-1,aһ��ȡ0.46
			// �˴�ȡ0.46
			// ʹ��Ƶ����ƽ��sin����
			fltHamm[i] = fltHamm[i] = (float)(0.54 - 0.46*Math.cos((2*i*PI1) / (FRM_LEN-1)));
		}
	}

	/***********************************
	 * �Ӵ� ��������AudHamming()
	 * ���ܣ��������ÿһ֡��֡������Ҫ���õ���õĺ�����ϵ����������ÿ���������ֵ���Ժ�����ϵ�����ٰѽ������fltFrame[]
	 ************************************/
	public void AudHamming() {
		for (int i = 0; i < FrmNum; i++) {
			// �Ӵ�
			for (int j = 0; i < FRM_LEN; i++) {
				// ���������ź��и�֡��Ӧ�ĺ�����ϵ��
				audFrame[i].fltFrame[j] *= fltHamm[j];
			}
		}
	}

	/***********************************
	 * ÿһ֡��ʱ���� ��������AudSte()
	 * ���ܣ���ÿһ֡�Ķ�ʱ����������������һ֡����������ֵ��ӣ�fpFrmSnd��֡��һ����
	 ************************************/

	public void AudSte() {	
		for (int i = 0; i < FrmNum; i++) {
			double fltShortEnergy = 0;
			for (int j = 0; j < FRM_LEN; j++) {
				fltShortEnergy += Math.abs(audFrame[i].fltFrame[j]);
			}
			audFrame[i].fltSte = fltShortEnergy;
		}
		
		
		
	}
	
	/***********************************
	*һ֡�Ĺ�����
	*��������AudZcr(fltSound *fpFrmSnd, DWORD FrmLen,fltSound ZcrThresh)
	*���ܣ����һ֡�Ĺ����ʣ�fpFrmSnd֡��һ���������ַ��FrmLen֡����ZcrThresh�����ʷ�ֵ
	************************************/
	public void AudZcr(){
		
		fltZcrVadThresh = 0.02;
		for( int i = 0; i < FrmNum; i++)
		{
			int    dwZcrRate = 0;
		for(int j =0 ; j < FRM_LEN - 1; j++)//����ʦ�ֺ����д�����ֵ��j-1
		{
			if((audFrame[i].fltFrame[j]*audFrame[i].fltFrame[j + 1] < 0)&&((audFrame[i].fltFrame[j] - audFrame[i].fltFrame[j + 1]) > fltZcrVadThresh))
				dwZcrRate++;
		}
		audFrame[i].dwZcr=dwZcrRate;
		}

		
	}
	
	
	/**********************************
	*����������ֵ
	*�������� AudNoiseEstimate����
	*���ܣ�����˫���޷�ֵ
    ***********************************/
	
	public void AudNoiseEstimate(){
		fltSteThresh = new double[2];
		dwZcrThresh = new double [2];
//		int ZcrThresh = 0;	//��������ֵ
//		double StrThresh =0.0;	//��ʱ������ֵ
//		int NoiseFrmLen = 0;
//		for(int i = 0; i < FrmNum; i++)   
//		{
//			ZcrThresh += audFrame[i].dwZcr;
//			StrThresh += audFrame[i].fltSte;		
//			NoiseFrmLen++;
//		}
//		dwZcrThresh[0] = (double)(ZcrThresh) / NoiseFrmLen;
//		dwZcrThresh[1] = (double)(ZcrThresh) / NoiseFrmLen*2.5;
//		fltSteThresh[0] = (double)StrThresh / NoiseFrmLen*0.7;
//		fltSteThresh[1] = (double)(StrThresh / NoiseFrmLen)*0.5;//*0.95;
		dwZcrThresh[0] = 10;
		dwZcrThresh[1] = 5;
		fltSteThresh[0] = 10;
		fltSteThresh[1] = 2;
		double maxSte = 0;
		for(int i = 0; i < FrmNum; i++)  {
			if(maxSte<audFrame[i].fltSte)
				maxSte = audFrame[i].fltSte;
		}
		
		fltSteThresh[0] = fltSteThresh[0]<(maxSte/4)?fltSteThresh[0]:(maxSte/4);
		fltSteThresh[1] = fltSteThresh[1]<(maxSte/8)?fltSteThresh[1]:(maxSte/8);
		
		
	}
	
	
	/***************************
	*�˵���
	*������: AudVadEstimate(void)
	*���ܣ��˵��⣬��Ҫ�õ����Ʒ�ֵ�ĺ��������ó���Ч��ʼ�����Ч��ֹ��
	*************************/
	
	public void AudVadEstimate(){
		//Extract Threshold
		double		ZcrLow=dwZcrThresh[1];
		double		ZcrHigh=dwZcrThresh[0];
		double	AmpLow=fltSteThresh[1];
		double	AmpHigh=fltSteThresh[0];
		WavStart=0;
		WavEnd=0;
		int status =0;
		int count =0;
		int silence =0;
		
		for(int i=0;i<FrmNum;i++)
		{
			switch(status)
			{
			case 0:
			case 1:
				if ((audFrame[i].fltSte)>AmpHigh)
				{
					WavStart = (i-count-1)>1?(i-count-1):1;
					status= 2;
					silence = 0;
					count = count + 1;
				}
				else if((audFrame[i].fltSte)>AmpLow || (audFrame[i].dwZcr)>ZcrLow)
				{
					status =1;
					count = count +1;
				}
				else
				{
				status=0;
				count =0;
				}
				break;

			case 2: //Speech Section

				if((audFrame[i].fltSte > AmpLow) || (audFrame[i].dwZcr > ZcrLow))
				{
					count = count +1;
					//WavEnd=i-Silence;
				}
				else
				{
					silence = silence+1;
					if (silence < MAX_SLIENCE_LEN) 
					{	
						count = count +1;
					}
					else if(count< MIN_WORD_LEN)   
					{	
						status  = 0;
						silence = 0;
						count = 0;
					}
					else
					{
						status = 3;
					}
				}
				break;
			default:
				break;
			}
			//��������֡
		}
		count = count-silence/2;
		WavEnd = WavStart + count -1;

//		try{
//        	FileWriter fileWriter=new FileWriter("d:\\javaresult.txt");
//        	          
//
//         fileWriter.write(String.valueOf(count)+" ");
//         fileWriter.write(String.valueOf(silence)+" ");
//        fileWriter.flush();
//        fileWriter.close();
//        }catch(Exception e){
//        	
//        }
	}
	/***************************
	*��һ��
	*������: vadCommon(void)
	*���ܣ����������й�һ��
	*************************/
	public void vadCommon(){
		for( int i = 0; i < dwSoundlen; i++)
		{
		if(maxData<Math.abs(data[i]))
			maxData=Math.abs(data[i]);
		}
		for( int i = 0; i < dwSoundlen; i++)
		{
			data[i] = data[i]/maxData;
		}
	}
//    public  double getNumber(double number){  
//        DecimalFormat df = new DecimalFormat("#.####");  
//        double f=Double.valueOf(df.format(number));  
//        return f;  
//    }  


	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
