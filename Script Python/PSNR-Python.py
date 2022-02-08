from math import log10, sqrt
import cv2
import numpy as np
from tkinter import Tk 
from tkinter.filedialog import askopenfilename
  
def PSNR(original, compressed):
	mse = np.mean((original - compressed) ** 2)
    
	if(mse == 0):  # MSE is zero means no noise is present in the signal.There fore PSNR have no importance.
		return 100
	max_pixel = 255.0
	psnr = 20 * log10(max_pixel / sqrt(mse))
	return psnr

def readImgPath():
	Tk().withdraw()
	filename = askopenfilename()

	return filename

def main():
	
	pathImg1 = readImgPath()
	pathImg2 = readImgPath()

	original = cv2.imread(pathImg1)
	compressed = cv2.imread(pathImg2)
	
	value = PSNR(original, compressed)
	print(f"PSNR value is {value} dB")#db decibels
       
if __name__ == "__main__":
	main()
