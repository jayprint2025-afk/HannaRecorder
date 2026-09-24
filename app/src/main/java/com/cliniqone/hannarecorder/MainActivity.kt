package com.cliniqone.hannarecorder

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class Task(val label:String,val phrase:String,val tip:String)

class MainActivity : AppCompatActivity() {
    private lateinit var prompt:TextView; private lateinit var kind:TextView; private lateinit var progress:TextView
    private lateinit var tip:TextView; private lateinit var record:Button; private lateinit var play:Button
    private lateinit var accept:Button; private lateinit var retry:Button; private lateinit var export:Button
    private var recorder:MediaRecorder?=null; private var current:File?=null; private var recording=false; private var index=0
    private val saved= mutableListOf<Pair<Task,File>>()
    private val tasks = buildList {
        repeat(10){ add(Task("positive", if(it<5) "Hanna" else "Oye Hanna","Dilo natural; cambia ligeramente distancia, volumen o velocidad.")) }
        listOf("Ana","Ana","Oye Ana","Oye Ana","Hermana","Hermana","Hola","Oye","Buenos días","¿Me escuchas?").forEach{add(Task("near-negative",it,"Di exactamente la frase. No agregues Hanna."))}
        repeat(10){ add(Task("positive", if(it<5) "Hanna" else "Oye Hanna","Dilo natural, como llamarías realmente al asistente.")) }
        listOf("Hola, buenas noches","Oye, ven","Buenas tardes","Abre la agenda","Tengo una cita mañana","¿Qué hora es?","Hola asistente","Oye asistente","Hermana, ven","Ana, ¿me escuchas?").forEach{add(Task("near-negative",it,"Di exactamente la frase. No agregues Hanna."))}
    }
    override fun onCreate(b:Bundle?){super.onCreate(b);setContentView(R.layout.activity_main)
        prompt=findViewById(R.id.prompt);kind=findViewById(R.id.kind);progress=findViewById(R.id.progress);tip=findViewById(R.id.tip)
        record=findViewById(R.id.record);play=findViewById(R.id.play);accept=findViewById(R.id.accept);retry=findViewById(R.id.retry);export=findViewById(R.id.export)
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this,arrayOf(Manifest.permission.RECORD_AUDIO),7)
        record.setOnClickListener{ if(recording) stop() else start() }
        play.setOnClickListener{ current?.let{MediaPlayer().apply{setDataSource(it.absolutePath);prepare();start()}}}
        retry.setOnClickListener{current?.delete();current=null;showTask()}
        accept.setOnClickListener{current?.let{saved.add(tasks[index] to it);index++;current=null;showTask()}}
        export.setOnClickListener{shareZip()}
        showTask()
    }
    private fun showTask(){
        if(index>=tasks.size){prompt.text="¡Terminaste!";kind.text="40 grabaciones completas";progress.text="Gracias por ayudar a entrenar a Hanna";tip.text="Pulsa Compartir ZIP por WhatsApp.";record.visibility=View.GONE;play.visibility=View.GONE;accept.visibility=View.GONE;retry.visibility=View.GONE;export.visibility=View.VISIBLE;return}
        val t=tasks[index]; progress.text="Grabación ${index+1} de ${tasks.size}"; prompt.text="“${t.phrase}”"
        kind.text=if(t.label=="positive")"POSITIVO — debe activar Hanna" else "NEGATIVO — NO debe activar Hanna";tip.text=t.tip
        record.visibility=View.VISIBLE;play.visibility=View.GONE;accept.visibility=View.GONE;retry.visibility=View.GONE;export.visibility=View.GONE
    }
    private fun start(){
        current=File(cacheDir,"take_${String.format("%02d",index+1)}.m4a")
        recorder=MediaRecorder().apply{setAudioSource(MediaRecorder.AudioSource.MIC);setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);setAudioEncoder(MediaRecorder.AudioEncoder.AAC);setAudioSamplingRate(16000);setAudioChannels(1);setOutputFile(current!!.absolutePath);prepare();start()}
        recording=true;record.text="Detener grabación"
    }
    private fun stop(){recorder?.stop();recorder?.release();recorder=null;recording=false;record.text="Grabar";play.visibility=View.VISIBLE;accept.visibility=View.VISIBLE;retry.visibility=View.VISIBLE}
    private fun shareZip(){
        val zip=File(cacheDir,"hanna_voice_${System.currentTimeMillis()}.zip")
        ZipOutputStream(FileOutputStream(zip)).use{z->
            saved.forEachIndexed{i,(t,f)->z.putNextEntry(ZipEntry("${t.label}_${String.format("%02d",i+1)}.m4a"));f.inputStream().copyTo(z);z.closeEntry()}
            z.putNextEntry(ZipEntry("manifest.csv"));z.write("index,label,phrase\n".toByteArray())
            saved.forEachIndexed{i,(t,_)->z.write("${i+1},${t.label},\"${t.phrase.replace("\"","\"\"")}\"\n".toByteArray())};z.closeEntry()
        }
        val uri=FileProvider.getUriForFile(this,"${packageName}.files",zip)
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply{type="application/zip";putExtra(Intent.EXTRA_STREAM,uri);addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)},"Enviar grabaciones"))
    }
}