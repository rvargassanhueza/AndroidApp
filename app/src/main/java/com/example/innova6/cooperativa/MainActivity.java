package com.example.innova6.cooperativa;
/*Desarrollado por Rodrigo A Vargas Sanhueza para Radio Cooperativa - Abril del 2016*/

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.larvalabs.svgandroid.SVG;
import com.larvalabs.svgandroid.SVGParser;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends Activity {

    Timer timer;
    boolean connectNew, connectOld;
    int TIME_WAIT_CHECK;//En milisegundos. Es el tiempo que pasará desde el inicio de la App para empezar a comprobar la
    // conexión.
    int TIME_CHECK; // En milisegundos. Cada cuento tiempo revisará la conexión.

    public static MediaPlayer mPlayer;

    ImageButton buttonPlay;
    ImageButton buttonPause;
    ProgressBar pgrbarr;
    //public String lk;
    //public static boolean flag = false;

    //*******Declaración de las tareas ejecutadas en segundo plano*****//

    //tarea1-> inicialización del player al presionar play, ademas de trabajar con el progressbar
    private MiTareaAsincrona tarea1;

    //tarea2-> inicialización del player al arrancar app
    private MiTareaAsincrona_2 tarea2;

    private FirebaseAnalytics mFirebaseAnalytics;

    String url = "http://unlimited3-cl.dps.live/cooperativafm/aac/icecast.audio";
    int media_lenght = 0;

    //private ProgressDialog dialog = new ProgressDialog(MainActivity.this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);// evita que se gire la pantalla

        // Iniciamos constantes
        TIME_WAIT_CHECK = Integer.parseInt(getResources().getString(R.string.time_wait_check));
        TIME_CHECK = Integer.parseInt(getResources().getString(R.string.time_check));

         // [START shared_tracker]
        // Obtener la instancia de FireBaseAnalytics.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        // [END shared_tracker]


        //Se define para agregar imagen SVG de barra en caso de telefonos con S.O > API 11
        if (android.os.Build.VERSION.SDK_INT>=11) {
            setContentView(R.layout.activity_main);

            buttonPlay = (ImageButton) findViewById(R.id.play);
            buttonPause = (ImageButton) findViewById(R.id.pause);
            pgrbarr=(ProgressBar) findViewById(R.id.progressBar);

            buttonPause.setVisibility(View.INVISIBLE);
            buttonPlay.setVisibility(View.INVISIBLE);

            pgrbarr.setVisibility(View.INVISIBLE);

            ImageView imageView = (ImageView) findViewById(R.id.binferior);//imageview de barra inferior
            ImageView imageView_play= (ImageView) findViewById(R.id.play);//imageview de boton play
            ImageView imageView_pause= (ImageView) findViewById(R.id.pause);//imageview de boton pause

            SVG homeSvg = SVGParser.getSVGFromResource(getResources(), R.raw.rep); //Parseo de imagen de barra inferior
            SVG homeSvg_play = SVGParser.getSVGFromResource(getResources(), R.raw.play); //Parseo de imagen de boton play
            SVG homeSvg_pause = SVGParser.getSVGFromResource(getResources(), R.raw.pause); //Parseo de imagen de boton stop

            imageView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);   //activa la aceleracion de hw
            imageView_play.setLayerType(View.LAYER_TYPE_SOFTWARE, null); //activa la aceleracion de hw
            imageView_pause.setLayerType(View.LAYER_TYPE_SOFTWARE, null); //activa la aceleracion de hw

            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            // imageView_play.setAdjustViewBounds(true);
            imageView.setImageDrawable(homeSvg.createPictureDrawable());

            imageView_play.setScaleType(ImageView.ScaleType.FIT_CENTER);
            imageView_play.setAdjustViewBounds(true);
            imageView_play.setImageDrawable(homeSvg_play.createPictureDrawable());

            imageView_pause.setScaleType(ImageView.ScaleType.FIT_CENTER);
            imageView_pause.setAdjustViewBounds(true);
            imageView_pause.setImageDrawable(homeSvg_pause.createPictureDrawable());

        }
        else
        {

            setContentView(R.layout.activity_main_bajo);

            buttonPlay = (ImageButton) findViewById(R.id.play);
            buttonPause = (ImageButton) findViewById(R.id.pause);
            pgrbarr=(ProgressBar) findViewById(R.id.progressBar);

            buttonPause.setVisibility(View.INVISIBLE);
            buttonPlay.setVisibility(View.VISIBLE);
            pgrbarr.setVisibility(View.INVISIBLE);
            //par.setVisibility(View.VISIBLE);
        }
        /************** Módulos de muestra de webview y validación de conectividad***************/
        mostrar_web();
        estaConectado();
        /************** /Módulos de muestra de webview validación de conectividad***************/

        mPlayer = new MediaPlayer();
        //para poder utilizar los botones de audio físicos
        mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        tarea2 = new MiTareaAsincrona_2();
        tarea2.execute();
        //Bloque de codigo para el streaming al presionar play
        buttonPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPlayer.setVolume(1,1);
                buttonPlay.setVisibility(View.INVISIBLE);
                buttonPause.setVisibility(View.VISIBLE);
                mPlayer.start();



            }
        });
        //Bloque de codigo para el streaming al presionar pause
        buttonPause.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                buttonPause.setVisibility(View.INVISIBLE);
                buttonPlay.setVisibility(View.VISIBLE);
                if (mPlayer != null && mPlayer.isPlaying()) {

                    //mPlayer.pause();
                    mPlayer.setVolume(0,0);

                }


            }
        });
        // Se inicia el TimerTask (una tarea en segundo plano que se ejecutará cada cierto tiempo
        // mientras esté activa la aplicación).
        networkConnected nc = new networkConnected();
        // Se inicia un timer necesario para que el TimerTask sepa cada cuanto tiempo se repetirá.
        timer = new Timer();
        timer.scheduleAtFixedRate(nc, TIME_WAIT_CHECK, TIME_CHECK);
        // 1.- Es el TimerTask que se ejecutará. uu
        // 2.- Es el tiempo que esperará para ejecutarse por primera vez.
        // 3.- Es el tiempo que tardará en repetirse el TimerTack.

        // ¡Imprescindible! Se encarga de guardar el estado de la antigua conexión y de la nueva. Llamaremos a este if ConectionOK, acuerdate.
        if (connectNew) {
            connectOld = false;
        } else {
            connectOld = true;
        }
    }
    public class MiTareaAsincrona extends AsyncTask<Void, Integer, Boolean> {
        @Override
        protected void onPreExecute() {
        }
        @Override
        protected void onPostExecute(Boolean aBoolean) {
            pgrbarr.setVisibility(View.INVISIBLE);
            buttonPause.setVisibility(View.VISIBLE);
            buttonPlay.setVisibility(View.INVISIBLE);
        }
        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        }
        @Override
        protected void onCancelled() {
            super.onCancelled();
        }
        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                //mPlayer.reset();
                mPlayer.setDataSource(url);
                mPlayer.prepare();
                mPlayer.setVolume(1,1);
                mPlayer.start();

            } catch (IOException e) {
                e.printStackTrace();
            }

            return true;
        }
    }
    private class MiTareaAsincrona_2 extends AsyncTask<Void, Integer, Boolean> {
        @Override
        protected void onPreExecute() {
        }
        @Override
        protected void onPostExecute(Boolean aBoolean) {
            pgrbarr.setVisibility(View.VISIBLE);

        }
        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        }
        @Override
        protected void onCancelled() {
            super.onCancelled();
        }
        @Override
        protected Boolean doInBackground(Void... params) {
            try {
               // mPlayer.reset();
                mPlayer.setDataSource(url);
                mPlayer.prepareAsync();
               // mPlayer.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
            //mp3 will be started after completion of preparing...
            mPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {

                @Override
                public void onPrepared(MediaPlayer meplayer) {
                    Log.i("Entra a onPrepared ","mPlayer.prepareAsync()");
                     meplayer.setVolume(0,0);
                     buttonPlay.setVisibility(View.VISIBLE);
                     buttonPause.setVisibility(View.INVISIBLE);
                     pgrbarr.setVisibility(View.INVISIBLE);
                     meplayer.start();

                }

            });
            return true;
        }
    }
    private void mostrar_web() {

        final WebView myBrowser;

        String url="http://m.cooperativa.cl";

        myBrowser = (WebView)findViewById(R.id.webView);

        myBrowser.getSettings().setJavaScriptEnabled(true);
        //myBrowser.setWebViewClient(new WebViewClient());
        myBrowser.setWebChromeClient(new WebChromeClient());
       // myBrowser.getSettings().setDomStorageEnabled(true); // Habilitar esta propiedad para poder reproducir los videos JWPlayer.

        myBrowser.setWebViewClient(new WebViewClient(){
        ProgressDialog prDialog;

            @Override
            public void onPageStarted (WebView view, String url, Bitmap favicon){

                prDialog = ProgressDialog.show(MainActivity.this,null,"Cargando");
                super.onPageStarted(view, url, favicon);
            }

            @Override
            public void onPageFinished(WebView view, String url){
                prDialog.dismiss();
                myBrowser.setWebViewClient(new WebViewClientExternal());
                super.onPageFinished(view, url);
                }
        });
        myBrowser.loadUrl(url);

    }
    @Override
    protected void onPause()
    {
        super.onPause();
    }
    protected void onResume() {

        super.onResume();
    }
    protected void onDestroy() {
        super.onDestroy();

    }
    @Override
    public void onBackPressed() {
        final WebView webView;
        webView = (WebView)findViewById(R.id.webView);

        if (webView.canGoBack()) {
            webView.goBack();
        }
        else
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Estás seguro que deseas salir de Cooperativa?")
                    .setCancelable(false)
                    .setPositiveButton("Si", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            mPlayer.setVolume(0,0);
                            finish();
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();
        }
    }
    /*Comprobamos si existe conexión a internet, si no existe se cargaran unas imágenes
        sustituyendo el WebView*/
    protected Boolean estaConectado(){
       ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context
                    .CONNECTIVITY_SERVICE);
            NetworkInfo info = connectivityManager.getActiveNetworkInfo();
            if (info == null || !info.isConnected() || !info.isAvailable()) { // No existe conexión
                Toast.makeText(getApplicationContext(), "No tienes conectividad a internet. Para usar la aplicación necesitas estar conectado", Toast.LENGTH_LONG).show();
                //Intent myIntent = new Intent(MainActivity.this, SinConexion.class);
                //startActivityForResult(myIntent, 0);
                return false;
            } else { // Existe conexión
                return true;
            }
        }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode == 0) {
            finish();
        }
    }

    // Tarea repetitiva en segundo plano. Se encarga de comprobar si la conexión se pierde o no.
    private class networkConnected extends TimerTask {
        @Override
        public void run() {
            // En los TimerTask no se puede hacer referencia a las views por lo que utilizamos
            // "runOnUiThread" para poder acceder a las view del thread principal.
            runOnUiThread(new Runnable() {
                              @Override
                              public void run() {
                                  // Comprobar si existe o no conexión a internet
                                  ConnectivityManager connectivityManager = (ConnectivityManager)
                                          getSystemService(Context.CONNECTIVITY_SERVICE);
                                  NetworkInfo info = connectivityManager.getActiveNetworkInfo();
                                  if (info == null || !info.isConnected() || !info.isAvailable()) {
                                      connectNew = false; // Desconectado
                                  } else {
                                      connectNew = true; // Conectado
                                  }

                                  // Cambiamos la vista de la actividad para mostrar una imagen (si
                                  // se pierde la conexión) o mostrar la web (si la hemos
                                  // recuperado).
                                  //
                                  // Utilizamos dos variables connectNew y connectOld:
                                  //
                                  //    - connectNew: esta variable es la que indicará si existe o
                                  //                  no conexión.
                                  //    - connectOld: esta variable detectará si se ha sufrido algún
                                  //                  cambio en la conexión desde el útlimo cambio.
                                  //
                                  // Cuando se ejecuta la aplicación, el valor de las dos variables
                                  // es null. Cuando se inicia la clase TimerTask, connectNew recibe
                                  // un valor booleano de la conexión y, como puede ser, que el valor
                                  // sea false en la primera ejecución, en el if ConnectionOK le damos a
                                  // connectOld el valor contrario al de connectNew para que entre
                                  // en la primera condición del IF siguiente (el de abajo de este parrafo).
                                  //
                                  // Esta primera condición sirve para detectar que ha habido un
                                  // cambio entre la conexión anterior y la nueva y por lo tanto la
                                  // vista de la actividad tiene que sufrir un cambio.
                                  //
                                  // Como en esta explicación hemos dado por sentado que la connectNew
                                  // seria false y connectOld seria true, entraría en la primera
                                  // condición del IF y luego solo se tendrá que comprobar el valor
                                  // connectNew para saber qué cambio es el que se tiene que
                                  // aplicar. En este caso false, desconectado.
                                  //
                                  // Por último, solo tenemos que guardar en connectOld este cambio
                                  // de conexión, por lo que connectOld se iguala a connectNew y se
                                  // vuelve a ejecutar la comprobación de la conexión, cambiando o
                                  // no el valor de connectNew. Además, como esta es la segunda vez
                                  // que ejecuta el código, ya no pasará por el if ConnectionOK para
                                  // cambiar el  valor booleano de connectOld. A partir de aquí,
                                  // connectOld solo cambiara su valor si connectNew cambia.

                                  if (connectNew != connectOld) {
                                      if (!connectNew) { //Desconectado.
                                          Toast.makeText(getApplicationContext(), "No tienes conectividad a internet. Para usar la aplicación necesitas estar conectado", Toast.LENGTH_LONG).show();

                                          Intent myIntent = new Intent(MainActivity.this, SinConexion.class);
                                          startActivityForResult(myIntent, 0);
                                      } else { //Conectado.
                                          Intent myIntent = new Intent(MainActivity.this, MainActivity.class);
                                          startActivityForResult(myIntent, 0);

                                      }
                                  }

                                  connectOld = connectNew;
                              }
                          }

            );
        }

    }

    /*Esta función es llamada desde mostrar_web, y permite abrir sitios externos.
    * Si está dentro de http o https, y si está dentro de las variables declaradas en el archivo
    * strings.xml (url_excluye_renvivo y url_excluye_programas )se abre con el navegador del SO por defecto,
    * de lo contrario se abre en el mismo webview.
    * Si no pertenece a http o https por ejemplo mailto, whatsapp, market,
    * se abre con la app nativa según el llamado que tenga programado en el web*/
    public class WebViewClientExternal extends WebViewClient {
         @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
          //Log.i("url_sitio: "+url_programas,"<--");

             if( url.startsWith("http") || url.startsWith("https") ) {
                 //variable para capturar lo declarado en el archivo strings.xml, para excluir radio_en_vivo y abrir en browser
                 boolean url_excluye_renvivo=Uri.parse(url).getPath().endsWith(view.getResources().getString(R.string.excluye_radio_en_vivo));
                 //variable para capturar lo declarado en el archivo strings.xml, para excluir los programas y abrirlos en browser
                 boolean url_excluye_programas=Uri.parse(url).getHost().endsWith(view.getResources().getString(R.string.excluye_programas));

                 if ( url_excluye_renvivo == true || url_excluye_programas == true ) {

                     Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                     view.getContext().startActivity(intent);
                     Log.i("Entra a if ","WebViewClientExternal_1");

                     return true;

                 }else{

                     if (Uri.parse(url).getHost().endsWith(view.getResources().getString(R.string.frag_web_root)))
                     {

                         Log.i("Entra a if ","WebViewClientExternal_2");
                     }
                    // return false;
                 }
                 return false;
             }else{


                 // Otherwise allow the OS to handle things like tel, mailto, etc.
                 Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                 startActivity( intent );
                 return true;

             }

         }
    }

   public static class ReceptorLlamadas extends BroadcastReceiver {
       @Override
       public void onReceive(Context context, Intent intent) {
           call(context);
       }

       private void call(Context context) {
           PhoneCallListener phoneListener = new PhoneCallListener();
           TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
           telephonyManager.listen(phoneListener, PhoneStateListener.LISTEN_CALL_STATE);
       }

       private class PhoneCallListener extends PhoneStateListener {
           public boolean isPhoneCalling = false;
           Boolean wasRinging = false;

           @Override
           public void onCallStateChanged(int state, String incomingNumber) {
               if (TelephonyManager.CALL_STATE_RINGING == state) {
                   // phone ringing
                  //Aquí ya detectas que el teléfono esta recibiendo una llamada entrante

               }
               if (TelephonyManager.CALL_STATE_OFFHOOK == state) {
                   // active
                   isPhoneCalling = true;

                   if (mPlayer != null && mPlayer.isPlaying()) {
                      mPlayer.start();
                      mPlayer.setVolume(0,0);

                   }
               }
               if (TelephonyManager.CALL_STATE_IDLE == state) {

                   isPhoneCalling = false;
               }

           }
       }
   }
}

