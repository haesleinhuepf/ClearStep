package demo;

import org.junit.Test;

import clearcl.ClearCLImage;
import clearcl.enums.ImageChannelDataType;
import clearcl.viewer.ClearCLImageViewer;
import fastfuse.FastFusionEngine;
import fastfuse.FastFusionMemoryPool;
import fastfuse.stackgen.StackGenerator;
import fastfuse.tasks.AverageTask;
import framework.Handler;
import simbryo.synthoscopy.microscope.lightsheet.drosophila.LightSheetMicroscopeSimulatorDrosophila;
import simulation.Simulator;

public class HandlerDemo {

	
	@Test
	public void createAndMeasureSimbryoStack() throws Exception
	{
		Handler lHandler = new Handler();
		
		boolean StDev = true;
		
		lHandler.InitializeModules(StDev);
		
		lHandler.mProgram1 = lHandler.mContext.createProgram(Simulator.class, "Calculator.cl");
		lHandler.mProgram1.addDefine("CONSTANT", "1");
		lHandler.mProgram1.buildAndLog();
			  
		// now that this is done, we initialize the time and create two images that will
		// be filled by the simulator during the run	  
		int lSize = 64;
			  
		int lPhantomWidth = lSize;
		int lPhantomHeight = lPhantomWidth;
		int lPhantomDepth = lPhantomWidth;
		      
		//hier drigend auf Datentyp achten
		ClearCLImage lImage = lHandler.mContext.createSingleChannelImage(ImageChannelDataType.UnsignedInt16, 
																lSize, lSize, lSize);
			  
		ClearCLImageViewer lViewImage = ClearCLImageViewer.view(lImage);
			  
		int lNumberOfDetectionArms = 2;
		int lNumberOfIlluminationArms = 4;
		int lMaxCameraResolution = lSize;
			  
		LightSheetMicroscopeSimulatorDrosophila lSimulator =
                      new LightSheetMicroscopeSimulatorDrosophila(lHandler.mContext,
                                                             		lNumberOfDetectionArms,
                                                             		lNumberOfIlluminationArms,
                                                             		lMaxCameraResolution,
                                                             		5f,
                                                             		lPhantomWidth,
                                                             		lPhantomHeight,
                                                             		lPhantomDepth);
			  
		StackGenerator lStackGenerator = new StackGenerator(lSimulator);
		
		FastFusionEngine lFastFusionEngine = new FastFusionEngine(lHandler.mContext);
		
		@SuppressWarnings("unused")
		FastFusionMemoryPool lMemoryPool = FastFusionMemoryPool.getInstance(lHandler.mContext,
                                                 							100 * 1024 * 1024, true);
		@SuppressWarnings("unused")
		ClearCLImageViewer lCameraImageViewer = lSimulator.openViewerForCameraImage(0);

		lSimulator.simulationSteps((int)lHandler.mTimeStepper.mStep/10);
		
		lSimulator.render(true);
		    
		lFastFusionEngine.addTask(new AverageTask("C0L0",
                    								"C0L1",
                    								"C0L2",
                    								"C0L3",
                    								"C0"));
		lFastFusionEngine.addTask(new AverageTask("C1L0",
                    								"C1L1",
                    								"C1L2",
                    								"C1L3",
                    								"C1"));
		lFastFusionEngine.addTask(new AverageTask("C0", "C1", "fused"));

		lStackGenerator.setCenteredROI(lSize, lSize);

		lStackGenerator.setLightSheetHeight(50f);
		lStackGenerator.setLightSheetIntensity(50f);

		for (int c = 0; c < 2; c++)
			for (int l = 0; l < 4; l++)
		    {
				String lKey = String.format("C%dL%d", c, l);

		        lStackGenerator.generateStack(c, l, -lSize/2f, lSize/2f, lSize);

		        lFastFusionEngine.passImage(lKey,
		                                      lImage);
		    }
		
		lFastFusionEngine.executeAllTasks();

		lImage.notifyListenersOfChange(lHandler.mContext.getDefaultQueue());

		lViewImage.waitWhileShowing();
		lSimulator.close();
		lStackGenerator.close();
	}
}