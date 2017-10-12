package timestepping;

import static java.lang.Math.pow;

import clearcl.ClearCLBuffer;
import clearcl.ClearCLContext;
import clearcl.ClearCLImage;
import clearcl.ClearCLKernel;
import clearcl.ClearCLProgram;
import clearcl.enums.ImageChannelDataType;
import coremem.enums.NativeTypeEnum;
import coremem.offheap.OffHeapMemory;

/**
 * This class can store two images and computes a metric for pixel-wise difference between the two
 * 
 * @author Raddock
 *
 */
public class Calculator {
	
	Setable mThres = new Setable();
	
	/**
	 * The first image that is stored by the calculator
	 */
	ClearCLImage mImage1=null;
	
	/**
	 * The second image that is stored by the calculator
	 */
	ClearCLImage mImage2=null;
	
	/**
	 * Is used to decide which cached picture will be overwritten
	 */
	boolean even = false;
	
	/**
	 * stores whether or not the calculator currently has two images stored
	 */
	boolean filled;

	
	/**
	 * receives an image and stores it into the Cache
	 * 
	 * @param image		The image received
	 * @param lContext	The OpenCL-Context
	 * @param lSize		The image-size to initialize an empty image if necessary
	 */
	public void CachePic(ClearCLImage image, ClearCLContext lContext, int lSize)
	{
		if (!even)
		{
			if (mImage1==null)
				// creates an empty picture if the cache is null
				{ mImage1 = lContext.createSingleChannelImage(ImageChannelDataType.Float, lSize, lSize, lSize); }
					
			image.copyTo(mImage1, false);
			even=true;
			System.out.println("image1 set");
		}
		else 
		{
			if (mImage2==null)
				// creates an empty picture if the cache is null
				{ mImage2 = lContext.createSingleChannelImage(ImageChannelDataType.Float, lSize, lSize, lSize); }
			
			image.copyTo(mImage2, false);
			even=false;
			System.out.println("image2 set");
		}
		if (mImage1 != null && mImage2 !=null)
			filled = true;
	}
	
	/**
	 * compares two images and responds with a metric that
	 * relates to the change between the two
	 * 
	 * @param lContext 	The OpenCL-Context
	 * @param lProgram 	The OpenCL-Program
	 * @param lSize 	The Size of the images
	 * @return 			The metric of change between the images
	 */
	public float compareImages(ClearCLContext lContext, ClearCLProgram lProgram, int lSize)
	{
		// creates a temporary image to store the pixel-wise difference
		ClearCLImage lImage = lContext.createSingleChannelImage(ImageChannelDataType.Float,
                                                   lSize,
                                                   lSize,
                                                   lSize);
		
		int lReductionFactor = 16;

	      ClearCLBuffer lEnd = lContext.createBuffer(NativeTypeEnum.Float,
	                                                 (int) pow(lReductionFactor,
	                                                           3));
	      ClearCLBuffer BufferMin = lContext.createBuffer(NativeTypeEnum.Float,
                  (int) pow(lReductionFactor,
                            3));
	      ClearCLBuffer BufferMax = lContext.createBuffer(NativeTypeEnum.Float,
                  (int) pow(lReductionFactor,
                            3));
		
		// runs the kernel for image-comparison
	    /**String lString = new String();
	    if (!mThres.set)
	    {
	    	lString = "compare";
	    	mThres.set=true;
	    }
	    else 
	    	lString = "compareAndFilter";*/
	    if (!mThres.set)
	    { 	
	    	mThres.val=5;
	    	mThres.set=true;
	    }  
	    ClearCLKernel lKernel = lProgram.createKernel("compareNFilter");
	    lKernel.setArgument("image1", mImage1);
	    lKernel.setArgument("image2", mImage2);
	    lKernel.setArgument("result", lImage);
	    lKernel.setArgument("threshold", mThres.val);
	    lKernel.setArgument("BufferMin", BufferMin);
	    lKernel.setArgument("BufferMax", BufferMax);
	    lKernel.setGlobalSizes(lReductionFactor, lReductionFactor, lReductionFactor);
	    lKernel.run(true);
	    
	    OffHeapMemory lBufferMin = OffHeapMemory.allocateFloats(BufferMin.getLength());
	    OffHeapMemory lBufferMax = OffHeapMemory.allocateFloats(BufferMax.getLength());
	    
	    BufferMin.writeTo(lBufferMin, true);
	    float max = 0;
    	float min = 99999;
	    for (int i = 0; i < BufferMin.getLength(); i++)
	    {
	    	float lFloatAligned = lBufferMin.getFloatAligned(i);
	    	if (lFloatAligned<min)
	    		min=lFloatAligned;
	    	if (lFloatAligned>max)
	    		max=lFloatAligned;
	    }
	    
	    mThres.val=min+((max-min)/10);
	    mThres.val=5;
		
	    // runs the kernel for summing up the "difference-Map" block-wise into an array
	    ClearCLKernel lKernel1 = lProgram.createKernel("Sum3D");
	    lKernel1.setArgument("image", lImage);
	    lKernel1.setArgument("result", lEnd);
	    lKernel1.setGlobalSizes(lReductionFactor, lReductionFactor, lReductionFactor);
	    lKernel1.run(true);

	    OffHeapMemory lBuffer = OffHeapMemory.allocateFloats(lEnd.getLength());

	    // copy the array from the kernel to a buffer and sum everything up
	    lEnd.writeTo(lBuffer, true);
	    float lDiff = 0;
	    for (int i = 0; i < lEnd.getLength(); i++)
	    {
	    	float lFloatAligned = lBuffer.getFloatAligned(i);
	    	lDiff += lFloatAligned;
	    }	
		return lDiff;
	}
}
