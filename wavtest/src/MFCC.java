import java.io.FileWriter;


public class MFCC {
	public static int LEN_PF_MFCC = 256;
	public static int SIZE_X_X_MFCC = 32;
	public static int SIZE_X_Y_MFCC = 140;
	public static int FS_MFCC = 8000;//HZ频率
	public static int N_MFCC = 256;//FFT的长度
	public static int SIZE_DCT_X_MFCC = 13;//dct系数二维数组x大小
	public static int SIZE_DCT_Y_MFCC = 25;//dct系数二维数组y大小
	public static double FH_MFCC = 0.5;
	public static double FL_MFCC = 0;
	public double buf[][]; 
	public double bank[][];//mel滤波器组系数
	public int Pos;
	public static int P_MFCC = 24;//mel滤波器组个数
	public static double pi = 3.1415926536;// 定义pi
	public double hamming[];//汉明窗
	public double working[];
	public double workingi[];
	public double dctcoef[][];//dct系数
	public double stren_win[];//归一化倒谱提升窗口
	public double result[];
	public int index1;
	public double m[][];
	public MFCC(){
		working = new double[257];
		workingi = new double[257];
		melbank(); 
		calcu_hamming(); 
		cal_stren_win(); 
		dct(); 
	}
	public void proc() 
	 { 
	     int i,j; 
	     double sum;      
	     double proc_buf1[] = new double[25]; 
	     buf = new double [5][25];
	     result = new double [25];
	     for(i = 256; i >= 1; i--)
		 {
			 working[i] = working[i]-working[i-1]; 
		 }
	     for(i = 1;i <= 256;i++)
		 {
			 //加窗——//s=y'.*hamming(256); 同时错一格。
			 working[i - 1] = working[i] * hamming[i]; 
		 }
		 //快速傅里叶变换——//t=abs(fft(s)); 
	     fft(); 
	     for(i = 256;i >= 1;i--) 
	     {
			 working[i] = working[i-1] * working[i-1] + workingi[i-1] * workingi[i-1]; 
	         workingi[i-1] = 0; 
	     } 
	     for(i = 1;i <= 24;i++) 
	     { 
	         sum = 0; 
	         for(j = 1;j <= 129;j++) 
	         { 
	             sum = sum + bank[i][j] * working[j]; 
	         } 
	         if(sum != 0)//求对数
			 {
				 proc_buf1[i] = Math.log(sum); 
			 }
	         else
			 {
				 proc_buf1[i] = -3000; 
			 }
	     } 
	     for(i = 1;i <= 12;i++) 
	     { 
	         sum = 0; 
	         for(j = 1;j <= 24;j++) 
	         { 
	             sum = sum + dctcoef[i][j] * proc_buf1[j]; //dctcoef是DCT系数
	         } 
	         buf[Pos][i] = sum * stren_win[i]; 
	     }  
	     Pos = (Pos+1) % 5; 
	     for(i = 1; i <= 12; i++)
		 { 
	         result[i] = buf[(Pos+2) % 5][i]; 
			 //dtm(i,:)=-2*m(i-2,:)-m(i-1,:)+m(i+1,:)+2*m(i+2,:); 
	         result[i+12] = -2 * buf[(Pos)%5][i] - buf[(Pos+1)%5][i] + buf[(Pos+3)%5][i] + 2*buf[(Pos+4)%5][i]; 
	         result[i+12] = result[i+12] / 3; 
	     }  
	 } 
	/***************************
	*mel滤波器组系数 ,ok
	*函数名: melbank(void)
	*功能：求mel滤波器组系数，并且对其进行归一化,主要参数P_MFCC，LEN_PF_MFCC，FS_MFCC，FH_MFCC，FL_MFCC
	*
	*************************/
public void melbank(){
	  double f0,fn2,lr; 
	     int b1,b2,b3,b4,k2,k3,k4,mn,mx; 
	     double bl[] = new double[5]; 
	     double pf[] = new double[LEN_PF_MFCC]; 
	     double fp[] = new double[LEN_PF_MFCC]; 
	     double	pm[] = new double[LEN_PF_MFCC]; 
	     double	 v[] = new double[LEN_PF_MFCC]; 
	     int r[] = new int [LEN_PF_MFCC]; 
	     int c[] = new int [LEN_PF_MFCC]; 
	     int i,j; 
	     bank = new double[SIZE_X_X_MFCC][SIZE_X_Y_MFCC];
	     f0 = (double)700 / (double)FS_MFCC;  
	     fn2 = Math.floor((double)N_MFCC/2); 
	     lr = Math.log((f0+FH_MFCC) / (f0+FL_MFCC)) / (P_MFCC+1.0); 
	     bl[1] = N_MFCC * ((f0 + FL_MFCC)*Math.exp(0*lr) - f0); 
	     bl[2] = N_MFCC * ((f0 + FL_MFCC)*Math.exp(1*lr) - f0); 
	     bl[3] = N_MFCC * ((f0 + FL_MFCC)*Math.exp(P_MFCC*lr) - f0); 
	     bl[4] = N_MFCC * ((f0 + FL_MFCC)*Math.exp((P_MFCC+1)*lr) - f0); 	  
	     b2 = (int)Math.ceil(bl[2]); 
	     b3 = (int)Math.floor(bl[3]); 
		 b1 = (int)Math.floor(bl[1]) + 1; 
	     b4 = (int)Math.min(fn2,Math.ceil(bl[4])) - 1; 
	     k2 = b2 - b1 + 1; 
	     k3 = b3 - b1 + 1; 
	     k4 = b4 - b1 + 1; 
	     mn = b1 + 1; 
	     mx = b4 + 1; 
	     for(i = 1,j = b1;j <= b4; i++,j++)
	     { 
	         pf[i] = Math.log(((double)f0 + (double)i / (double)N_MFCC) / (f0+FL_MFCC)) / lr;  
	         fp[i] = Math.floor(pf[i]); 
	         pm[i] = pf[i] - fp[i]; 
	     } 
	     for(i = 1,j = k2;j <= k4; i++,j++)
		 { 
	         r[i] = (int)fp[j]; 
	         c[i] = j;  
	         v[i] = 2 * (1 - pm[j]); 
	     } 
	     for(j = 1;j <= k3; j++,i++)
		 { 
	         r[i] = 1 + (int)fp[j]; 
	         c[i] = j; 
	         v[i] = 2 * pm[j]; 
	     } 
	     for(j = 1;j < i;j++)
		 { 
	         v[j] = 1 - 0.92 / 1.08 * Math.cos(v[j]*pi / 2); 
	         bank[r[j]][c[j]+mn-1] = v[j]; 
	     } 
	     
	     //bank=bank/max(bank(:));
	     double buf = 0; 
	     for(i = 1;i <= 24;i++) 
	     for(j = 1;j <= 129;j++) 
		 {
	         if(bank[i][j] > buf)buf = bank[i][j];
		 }
	  
	     for(i = 1;i <= 24;i++) 
		 {
	         for(j = 1;j <= 129;j++) 
			 {
	        	 bank[i][j] = bank[i][j] / buf; 
			 }
		 }
	     
	    
	     
}


/***************************
*快速傅里叶变换
*函数名: fft(void)
*功能：
*************************/
public void fft(){
	 int i,j = 0,k,n,l,le,le1,ip,sign = -1; 
     double tr,ti,ur,ui,wr,wi; 
     n = 1<<8; //n = 256
     for(i = 0;i < n - 1;i++) 
     { 
         if(i < j)
		 { 
             tr = working[j]; 
             working[j] = working[i];
             working[i] = tr;
/*
			 ti = xi[j];
             xi[j] = xi[i]; 
             xi[i] = ti;*/ 
         } 
         k = n >> 1; //相当于n/2
         while(k <= j)
		 { 
             j -= k; 
             k = k >> 1; 
         } 
         j += k; 
     } 
     for(l = 1;l <= 8;l++) 
     {
         le = 1 << l; 
         le1 = le / 2; 
         ur = 1.; 
         ui = 0.; 
         wr = Math.cos(pi/le1); 
         wi = -Math.sin(pi/le1); 
         for(j = 0;j < le1;j++) 
         { 
			 for(i = j;i < n;i += le) 
             { 
                 ip = i + le1; 
                 tr = working[ip] * ur - workingi[ip] * ui; 
                 ti = working[ip] * ui + workingi[ip] * ur; 
                 working[ip] = working[i] - tr; 
                 workingi[ip] = workingi[i] - ti; 
                 working[i] = working[i] + tr; 
                 workingi[i] = workingi[i] + ti; 
             } 
             tr = ur * wr - ui * wi; 
             ti = ur * wi + ui * wr; 
             ur = tr; 
             ui = ti; //可以简化
         } 
     } 
}


/***************************
*离散余弦变换DCT,ok
*函数名: dct(void)
*功能：求DCT系数
*************************/
public void dct(){
	dctcoef = new double [SIZE_DCT_X_MFCC][SIZE_DCT_Y_MFCC];
	 for(int k = 1;k <= 12;k++)
	 {
		for(int n = 0;n <= 23;n++) 
		{ 
			dctcoef[k][n+1] = Math.cos((double)(2*n+1)*k*pi / (double)(2*24)); 
		} 
	 }
	 
	
}


/***************************
*归一化倒谱提升窗口,ok
*函数名: cal_stren_win(void)
*功能：
*************************/
public void cal_stren_win(){
	stren_win = new double[13];
	 double b = 0.0; 
     for(int i = 1;i <= 12;i++)
	 { 
         stren_win[i] = 1 + 6 * Math.sin(pi * (double)i / (double)12); 
         if(b < stren_win[i])
		 {
			 b = stren_win[i]; 
		 }
     } 
     for(int i = 1;i <= 12;i++) //归一化
	 { 
         stren_win[i] = stren_win[i] / b; 
     } 
     
	
}
/***************************
*汉明窗
*函数名: calcu_hamming(void)
*功能：
*************************/
public void calcu_hamming() 
{ 
	hamming = new double[257];
    for(int i = 1;i <= 256;i++)
	 { 
        hamming[i] = 0.54 - 0.46 * Math.cos(2 * pi * (i-1)/(256-1)); 
    } 
} 
public void getMfcc(String path)
{

	 Vad vad = new Vad(path);
	 vad.AudPreEmphasize();
	 vad.WaveEndtest();
	 m = new double[vad.FrmNum][24];
	 index1=0;
	 for(int i= vad.WavStart;i<=vad.WavEnd;i++)
	 {
		 for (int j = 0; j < 257; j++)
		 {
			 working[j] = (double)vad.fpData[i*128+j];
		 }
		
        proc();
		 if(i-vad.WavStart>1 && vad.WavEnd-i>1)
		 {
			 for(int j=1;j<25;j++)
			 {
				 m[index1][j-1]=result[j];
			 }
			 index1++;
		 }
	 }
	 
	 

}
}
