import java.io.FileWriter;
import java.text.DecimalFormat;



public class Vad {

	public static double PI1 = 3.1415926536;// 定义pi
	public static int FRM_LEN = 256;// 定义帧长
	public static int FRM_SFT = 80;// 定义帧移
	public static double CHANGE = Math.pow(2, 15);
	public int FrmNum;// 总共多少帧
	public int dwSoundlen;// 采样点个数
	public int predata[];// 除以15次方，化归一化前采样点
	public double data[];//除以15次方，化归一化后采样点
	public double fpData[];// 预加重后的采样点
	public float fltHamm[];// Hamming窗系数
	public AudFrame audFrame[];// 帧数组
	public double fltZcrVadThresh;//过零率 阀值,0.02
	double fltSteThresh[];     //双门限短时能量阈值[0]高[1]低   
	double	dwZcrThresh[];  //双门限过零率阈值[0]高[1]低   
	int   WavStart;//语音其实点
	int   WavEnd;//语音结束点
	public static int  MIN_WORD_LEN=15;//最小语音长度
	public static int  MAX_SLIENCE_LEN=8; 	//最大静音长度
	//public static int WORD_MAX_SLIENCE =10;  //语音间最大静音距离
	public double dwWordLen;//端点检测后语音段长度
	public double maxData;//最大的采样点
	public Vad(String filename) {
		 ReadWav(filename);
		// AudPreEmphasize();//预加重
//		 AunEnframe();//分帧
//		 Hamming() ;//求hamming系数
//		AudHamming();//加窗
//		AudSte();//求解短时能量
//		AudZcr();//求解过零率
//		AudNoiseEstimate();	//估计噪声阈值
//		AudVadEstimate();//端点检测
	}
	
	/***************************
	*MFCC用的端点检测
	*函数名: WaveEndtest(void)
	*功能：端点检测
	*************************/
	public void WaveEndtest() 
	{
		AunEnframe();//分帧
		Hamming() ;//求hamming系数
		AudHamming();//加窗
		AudSte();//求解短时能量
		AudZcr();//求解过零率
		AudNoiseEstimate();	//估计噪声阈值
		AudVadEstimate();//端点检测
	}
	
	

	/***********************************
	 * 读取音频 函数名：ReadWav() 功能：读取音频
	 ************************************/
	public void ReadWav(String filename) {
		
		WaveFileReader reader = new WaveFileReader(filename);
		if (reader.isSuccess()) {
			predata = reader.getData()[0]; // 获取第一声道
			dwSoundlen = predata.length;
		} else {
			System.err.println(filename + "不是一个正常的wav文件");
		}
		data = new double[dwSoundlen];
		for(int i=0;i<dwSoundlen;i++){
			data[i]= predata[i]/CHANGE;
		}
		
		vadCommon();//归一化
		
		
		
	}

	/***********************************
	 * 预加重 函数名：AudPreEmphasize() 功能：对所有采样点进行预处理
	 *  % 预加重滤波器 
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
	 * 分帧 函数名：AudEnframe() 功能：给每一帧的fltFrame[FRM_LEN]赋采样点的值，个数是帧长
	 * fpData是预加重后的数据
	 ************************************/
	public void AunEnframe() {
		FrmNum = (dwSoundlen - (FRM_LEN - FRM_SFT)) / FRM_SFT;
		audFrame = new AudFrame[FrmNum];
		for(int i=0;i<FrmNum;i++){
			audFrame[i] = new AudFrame();
		}
		int x = 0;// 每一帧的起始点
		for (int i = 0; i < FrmNum; i++) {
			audFrame[i].fltFrame = new double[FRM_LEN];
			
			for (int j = 0; j < FRM_LEN; j++) {
				audFrame[i].fltFrame[j] = data[x + j];
			}
			x+=FRM_SFT;
		}
		
		
	}

	/***********************************
	 * 汉明窗系数 函数名：Hamming()
	 * 功能：求汉明窗系数，输入的是每一帧的帧长，要用到PI。这个数组是固定值，只有帧长决定
	 ************************************/
	public void Hamming() {
		fltHamm = new float[FRM_LEN];
		for (int i = 0; i < FRM_LEN; i++) {
			// 汉明窗函数为W(n,a) = (1-a) -αcos(2*PI*n/(N-1))
			// 0≦n≦N-1,a一般取0.46
			// 此处取0.46
			// 使音频波段平滑sin（）
			fltHamm[i] = fltHamm[i] = (float)(0.54 - 0.46*Math.cos((2*i*PI1) / (FRM_LEN-1)));
		}
	}

	/***********************************
	 * 加窗 函数名：AudHamming()
	 * 功能：输入的是每一帧的帧长，需要利用到求得的汉明窗系数，具体是每个采样点的值乘以汉明窗系数，再把结果赋予fltFrame[]
	 ************************************/
	public void AudHamming() {
		for (int i = 0; i < FrmNum; i++) {
			// 加窗
			for (int j = 0; i < FRM_LEN; i++) {
				// 保存语音信号中各帧对应的汉明窗系数
				audFrame[i].fltFrame[j] *= fltHamm[j];
			}
		}
	}

	/***********************************
	 * 每一帧短时能量 函数名：AudSte()
	 * 功能：求每一帧的短时能量，即将所有这一帧的所有样点值相加，fpFrmSnd是帧第一个样
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
	*一帧的过零率
	*函数名：AudZcr(fltSound *fpFrmSnd, DWORD FrmLen,fltSound ZcrThresh)
	*功能：求解一帧的过零率，fpFrmSnd帧第一个采样点地址，FrmLen帧长，ZcrThresh过零率阀值
	************************************/
	public void AudZcr(){
		
		fltZcrVadThresh = 0.02;
		for( int i = 0; i < FrmNum; i++)
		{
			int    dwZcrRate = 0;
		for(int j =0 ; j < FRM_LEN - 1; j++)//智明师兄后面有带绝对值，j-1
		{
			if((audFrame[i].fltFrame[j]*audFrame[i].fltFrame[j + 1] < 0)&&((audFrame[i].fltFrame[j] - audFrame[i].fltFrame[j + 1]) > fltZcrVadThresh))
				dwZcrRate++;
		}
		audFrame[i].dwZcr=dwZcrRate;
		}

		
	}
	
	
	/**********************************
	*估计噪声阀值
	*函数名： AudNoiseEstimate（）
	*功能：计算双门限阀值
    ***********************************/
	
	public void AudNoiseEstimate(){
		fltSteThresh = new double[2];
		dwZcrThresh = new double [2];
//		int ZcrThresh = 0;	//过零率阈值
//		double StrThresh =0.0;	//短时能量阈值
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
	*端点检测
	*函数名: AudVadEstimate(void)
	*功能：端点检测，需要用到估计阀值的函数，最后得出有效起始点和有效截止点
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
			//更新语音帧
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
	*归一化
	*函数名: vadCommon(void)
	*功能：对语音进行归一化
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
