# java版的语音预处理以及提起MFCC参数的程序

过程包括了语音信号处理，预加重，分帧加窗，端点检测与求MFCC  

## 定义PI的值

`const float PI1=3.1415926536;`

## 帧的结构体

`typedef struct _AudFrame
{
	float fltFrame[FRM_LEN];
	float fltSte;	 //这一帧的短时能量
	DWORD	 dwZcr;	 //这一帧的过零率
	bool	 blVad;//判断这帧是否有效
	struct _AudFrame * AudFrmNext;//下一帧的地址，不知道python有没有地址
}AudFrame;`


## 读取音频  

函数名：ReadWav(CString strFileName)  

功能：读取音频  
  
## 预加重  

函数名：AudPreEmphasize(void)  

功能：对所有采样点进行预处理  

解释：  
spSound：处理前的采样数据  
fpPreSound：处理后的采样数据  
`fpPreSound[i] = (float)(spSound[i]) - float(spSound[i - 1]) * 0.9375;`

## 分帧  

函数名：AudEnframe(float *Sound,DWORD FrmLen,DWORD FrmSft,DWORD dwSoundLen)

功能：给每一帧的fltFrame[Frmlen]赋采样点的值，个数是帧长

解释：
Sound 输入的采样点数据的起始地址
Frmlen是帧的长度
FrmSft是帧移
dwSoundlen是采样点长度
FrmNum表示有多少帧
由采样点长度，帧长，帧移得到帧的个数FrmNum
FrmNum = (dwSoundLen - (FrmLen - FrmSft)) / FrmSft;


## 汉明窗系数  

函数名：Hamming(DWORD FrmLen)  

功能：求汉明窗系数，输入的是每一帧的帧长，要用到PI。这个数组是固定值，只有帧长决定  

解释：  
FrmLen 帧长，固定值  
fltHamm[i] = (float)(0.54 - 0.46*cos((2*i*PI1) / (FrmLen-1)));



## 加窗  

函数名：AudHamming(DWORD FrmLen)  

功能：输入的是每一帧的帧长，需要利用到求得的汉明窗系数，具体是每个采样点的值乘以汉明窗系数，再把结果赋予fltFrame[]  

解释：  
需要先求得汉明窗系数fltHamm[i];
(stpWav->fltFrame)[i] 每一帧采样点数据
for(DWORD i = 0; i < FrmLen; i++)
		{
						(stpWav->fltFrame)[i] *= fltHamm[i];
		}




## 每一帧短时能量  

函数名：AudSte(fltSound *fpFrmSnd, DWORD FrmLen) 

功能：求每一帧的短时能量，即将所有这一帧的所有样点值相加，fpFrmSnd是帧第一个样本值  

解释:  
fltShortEnergy：每一帧短时能量  
fpFrmSnd：每个样本点的值
for(int i = 0; i < FrmLen; i++)
	{
		fltShortEnergy += fabs(*fpFrmSnd++);
	}


## 一帧的过零率  

函数名：AudZcr(fltSound *fpFrmSnd, DWORD FrmLen,fltSound ZcrThresh)  

功能：求解一帧的过零率，fpFrmSnd帧第一个采样点地址，FrmLen帧长，ZcrThresh过零率阀值  

解释：  
 
##fpFrmSnd样本点的值
DWORD CVad::AudZcr(fltSound *fpFrmSnd, DWORD FrmLen,fltSound ZcrThresh)
{
	DWORD    dwZcrRate = 0;

	for(int i = 0; i < FrmLen - 1; i++)
	{
		if((fpFrmSnd[i]*fpFrmSnd[i + 1] < 0)&&(fabs(fpFrmSnd[i] - fpFrmSnd[i - 1]) > ZcrThresh))
			dwZcrRate++;
	}
	return dwZcrRate;
}



## 估计噪声阀值  

函数名： AudNoiseEstimate（）  

功能：计算双门限阀值  

解释：
fltSteThresh [2] 短时能量阀值，[0]高 [1]低
dwZcrThresh [2]  过零率阀值， [0]高 [1]低
	ZcrThresh = 0;	
	StrThresh = 0.0;	
	ZcrThresh = 所有帧的过零率之和
	StrThresh = 所有帧的短时能量之和
	NoiseFrmLen 信号帧数
	dwZcrThresh[0] = (float)(ZcrThresh) / NoiseFrmLen;
	dwZcrThresh[1] = (float)(ZcrThresh) / NoiseFrmLen*2.5;
	fltSteThresh[0] = (float)StrThresh / NoiseFrmLen*0.7;
	fltSteThresh[1] = (float)(StrThresh / NoiseFrmLen)*0.5;//*0.95;

## 端点检测  

函数名: AudVadEstimate(void)  

功能：端点检测，需要用到估计阀值的函数，最后得出有效起始点和有效截止点  


