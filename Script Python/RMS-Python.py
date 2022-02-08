from PIL import Image, ImageStat
from tkinter import Tk 
from tkinter.filedialog import askopenfilename


def RMS(filename):
	im = Image.open(filename)
	stat = ImageStat.Stat(im)
	
	return stat.rms

def main():
	Tk().withdraw()
	filename = askopenfilename()

	indiceRMS = RMS(filename)

	path = filename.split("/")

	Color = ["Red", "Green", "Blue"] 
	
	strng = ""

	if(len(indiceRMS) == 1):
		strng += "Indice RMS "
	else:
		strng += "Indici RMS "

	strng += str(path[len(path)-1]) + ":"

	if(len(indiceRMS) == 1):
		strng += "\n Gray :" + str(indiceRMS[0]) + " "
	else:
		for i in range(len(indiceRMS)):
			strng += "\n" + str(Color[i]) + " : " + str(indiceRMS[i]) + " "
			
	print(strng)

main()
