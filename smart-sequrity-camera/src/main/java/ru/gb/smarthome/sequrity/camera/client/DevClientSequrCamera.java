package ru.gb.smarthome.sequrity.camera.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.SocketUtils;
import ru.gb.smarthome.common.smart.IConsolReader;
import ru.gb.smarthome.common.smart.structures.Abilities;
import ru.gb.smarthome.common.smart.structures.Task;
import ru.gb.smarthome.empty.client.TaskExecutor;
import ru.gb.smarthome.empty.complex.DevClientEmptyComplex;
import ru.gb.smarthome.sequrity.camera.PropManagerSequrCamera;

import javax.annotation.PostConstruct;
import java.util.concurrent.Executors;

import static java.lang.String.format;
import static ru.gb.smarthome.common.FactoryCommon.CANNOT_SLEEP;
import static ru.gb.smarthome.common.smart.enums.DeviceTypes.SEQURITY_CAMERA;
import static ru.gb.smarthome.sequrity.camera.FactorySequrCamera.*;
import static ru.gb.smarthome.sequrity.camera.SequrityCameraApp.DEBUG;

@Component
@Scope ("prototype")
public class DevClientSequrCamera extends DevClientEmptyComplex
{
    @Autowired
    public DevClientSequrCamera (PropManagerSequrCamera pmsc) {
        super(pmsc);
    }

    @PostConstruct
    @Override public void init ()
    {
        abilities = new Abilities(
                SEQURITY_CAMERA,
                propMan.getName(),
                propMan.getUuid(),
                CANNOT_SLEEP)
                .setMaster (true)
                .setTasks (propMan.getAvailableTasks())
                .setSensors (propMan.getAvailableSensors())
                .setSlaveTypes (propMan.slaveTypes())
                ;
        sensorsNumber = abilities.getSensors().size();

        taskExecutorService = Executors.newSingleThreadExecutor (r->{
                        Thread t = new Thread (r);
                        t.setDaemon (true);
                        return t;});
        //if (DEBUG) {
            Thread threadConsole = new Thread (()->IConsolReader.runConsole (this));
            threadConsole.setDaemon(true);
            threadConsole.start();
        //}
        state.setVideoImageSource (VIDEO_IMAGE_BANNER_SRC);
    }

    @Override protected TaskExecutor launchTask (Task t)
    {
        if (t.getName().equals (TASKNAME_STREAMING))
        {
            int port = SocketUtils.findAvailableTcpPort();;
            CameraStreamExecutor cameraStreamExecutor =
                    new CameraStreamExecutor (t, new SmartCamera (cameraNamePrefix_, port));

            state.setVideoImageSource (format (VIDEO_IMAGE_SRC_FORMAT, port));
            return cameraStreamExecutor;
        }
        else if (DEBUG) throw new UnsupportedOperationException ();
        return null;
    }

    @Override protected void onTaskEndOrInterrupted ()
    {
        super.onTaskEndOrInterrupted();
        state.setVideoImageSource (VIDEO_IMAGE_BANNER_SRC);
    }
}
