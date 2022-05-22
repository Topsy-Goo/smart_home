package ru.gb.smarthome.sequrity.camera.client;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamStreamer;

import java.awt.*;

import static ru.gb.smarthome.common.FactoryCommon.*;
import static ru.gb.smarthome.sequrity.camera.FactorySequrCamera.WEBCAM_DETECTION_TIMEOUT;
import static ru.gb.smarthome.sequrity.camera.SequrityCameraApp.DEBUG;

public class SmartCamera
{
    public  static final boolean START = true;
    public  static final double  FPS   = 10.0;
    private              Webcam  camera;
    private        final int     port;
    private              WebcamStreamer streamer;


    public SmartCamera (String webcamNamePrefix, int po) {
        port = po;
        camera = findWebcamOrDefault (webcamNamePrefix);
    }

    private Webcam findWebcamOrDefault (String webcamNamePrefix)
    {
        Webcam webCam = null;
        if (isStringsValid(webcamNamePrefix)) {
            for (Webcam w : Webcam.getWebcams())
            {
                if (w.getName().startsWith (webcamNamePrefix)) {
                    webCam = w;
                    break;
        }   }   }
        //else webCam = Webcam.getDefault();

        try {
            if (webCam == null)
                webCam = Webcam.getDefault (WEBCAM_DETECTION_TIMEOUT);

            webCam.setViewSize (new Dimension (320, 240));
            Dimension dimension = webCam.getViewSize();

            lnprintf ("Найдена камера «%s»: установлено разрешение: %d x %d.",
                      webCam.getName(), dimension.width, dimension.height);
            return webCam;
        }
        catch (Exception e) { lnerrprintln ("Камера не найдена."); }
        return null;
    }

    public boolean start ()
    {
        if (camera != null && streamer == null)
        {
            streamer = new WebcamStreamer (port, camera, FPS, START);  //< WebcamStreamer(…) сам сделает camera.open(), если указать START.
            return true;
        }
        if (DEBUG) throw new RuntimeException ("А чего у нас камера всё время включена ?!");
        return false;
    }

    public void stop () {
        if (streamer != null)
            streamer.stop(); //< Это делает Webcam.close().
        streamer = null;
        camera = null;
    }
}
