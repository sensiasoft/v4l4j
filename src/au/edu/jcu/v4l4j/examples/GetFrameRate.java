/*
* Copyright (C) 2007-2008 Gilles Gigan (gilles.gigan@gmail.com)
* eResearch Centre, James Cook University (eresearch.jcu.edu.au)
*
* This program was developed as part of the ARCHER project
* (Australian Research Enabling Environment) funded by a   
* Systemic Infrastructure Initiative (SII) grant and supported by the Australian
* Department of Innovation, Industry, Science and Research
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public  License as published by the
* Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
* or FITNESS FOR A PARTICULAR PURPOSE.  
* See the GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see <http://www.gnu.org/licenses/>.
*
*/

package au.edu.jcu.v4l4j.examples;

import java.io.IOException;
import java.util.List;
import java.util.Vector;

import au.edu.jcu.v4l4j.Control;
import au.edu.jcu.v4l4j.FrameGrabber;
import au.edu.jcu.v4l4j.ImageFormat;
import au.edu.jcu.v4l4j.V4L4JConstants;
import au.edu.jcu.v4l4j.VideoDevice;
import au.edu.jcu.v4l4j.exceptions.ControlException;
import au.edu.jcu.v4l4j.exceptions.V4L4JException;

public class GetFrameRate {
	public static void main(String[] args) throws V4L4JException, IOException {
		List<Control> ctrls;
		String dev;
		int w, h, std, channel, qty, fmt, captureLength = 10;
		//Check if we have the required args
		//otherwise put sensible values in
		try {
			dev = args[0];
		} catch (Exception e){
			dev = "/dev/video0";
		}
		try {
			w = Integer.parseInt(args[1]);
		} catch (Exception e){
			w = V4L4JConstants.MAX_WIDTH;
		}
		try{			
			h = Integer.parseInt(args[2]);
		} catch  (Exception e) {
			h = V4L4JConstants.MAX_HEIGHT;
		}
		try {
			std = Integer.parseInt(args[3]);
		} catch (Exception e) {
			std = 0;
		}
		try {
			channel = Integer.parseInt(args[4]);
		} catch (Exception e){
			channel = 0;
		}
		try {
			qty = Integer.parseInt(args[5]);
		} catch (Exception e){
			qty = 80;
		}
		
		try {
			fmt = Integer.parseInt(args[6]);
		} catch (Exception e){
			fmt = -1;
		}
		

		long start=0, now=0;
		int n=0;
		FrameGrabber f = null;
		VideoDevice vd = null;

		System.out.println("This program will open "+dev+", list the available control, capture frames for "
					+ captureLength+ " seconds and print the FPS");
		System.out.println("Make sure the webcam is connected and press <Enter>, or Ctrl-C to abort now.");
		System.in.read();

		try {
			vd = new VideoDevice(dev);
			if(vd.canJPEGEncode() && qty!=-1 && fmt==-1) {
				f= vd.getJPEGFrameGrabber(w, h, channel, std, qty);
				System.out.println("Frames from this device can be JPEG-encoded");
				System.out.println("Image format: JPEG");
				
			} else {
				if(vd.canJPEGEncode())
					System.out.println("Frames from this device can be JPEG-encoded but disabled from command line (quality = "+qty+", format = "+fmt+")");
				else
					System.out.println("Frames from this device can *NOT* be JPEG-encoded");
				
				if(fmt==-1){
					f= vd.getRawFrameGrabber(w, h, channel, std);
					System.out.println("Image format: "+vd.getDeviceInfo().getFormats().get(0).getName());
				} else {
					for(ImageFormat im: vd.getDeviceInfo().getFormats())
						if(im.getIndex()==fmt)
							f= vd.getRawFrameGrabber(w, h, channel, std,im);
					
					if(f==null) {
						vd.release();
						throw new V4L4JException("Image format "+fmt+" not supported by this device");
					} else
						System.out.println("Image format: "+f.getImageFormat().getName());
					
				}
			}
		} catch (V4L4JException e) {
			e.printStackTrace();
			System.out.println("Failed to instanciate the FrameGrabber ("+dev+")");
			throw e;
		}
		
		ctrls = new Vector<Control>(vd.getControlList().getList());

		System.out.println("Found "+ctrls.size()+" controls");
		for (Control c: ctrls) {
			try {
				System.out.println("control name: "+c.getName()+" - min: "+c.getMinValue()+" - max: "+c.getMaxValue()+" - step: "+c.getStepValue()+" - value: "+c.getValue());
			} catch (ControlException e) {
				e.printStackTrace();
				System.out.println("Failed to get value for control "+c.getName());
			}
		}
		vd.releaseControlList();

		try {
			f.startCapture();
		} catch (V4L4JException e) {
			e.printStackTrace();
			System.out.println("Failed to start capture");
			vd.releaseFrameGrabber();
			vd.release();
			throw e;
		}

		try {
			System.out.println("Starting test capture at "+f.getWidth()+"x"+f.getHeight()+" for "+captureLength+" seconds");
			now=start=System.currentTimeMillis();
			while(now<start+(captureLength*1000)){
				f.getFrame();
				//Uncomment the following to dump the captured frame to a jpeg file
				//also import java.io.FileOutputStream 
				//new FileOutputStream("file"+n+".jpg").getChannel().write(f.getFrame());
				n++;
				now=System.currentTimeMillis();
			}
		} catch (V4L4JException e) {
			e.printStackTrace();
			System.out.println("Failed to perform test capture");
			f.stopCapture();
			vd.releaseFrameGrabber();
			vd.release();
			throw e;
		}

		System.out.println(" =====  TEST RESULTS  =====");
		System.out.println("\tFrames captured :"+n);
		System.out.println("\tFPS: "+((float) n/(now/1000-start/1000)));
		System.out.println(" =====  END  RESULTS  =====");

		f.stopCapture();
		vd.releaseFrameGrabber();
		vd.release();
	}
}