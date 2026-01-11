package com.androos.player

import android.Manifest
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val path: String,
    val albumId: Long
)

class MusicRepository(private val context: android.content.Context) {
    suspend fun loadSongs(): List<Song> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Song>()
        
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ALBUM_ID
        )
        
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND (" +
                "${MediaStore.Audio.Media.MIME_TYPE} = ? OR " +
                "${MediaStore.Audio.Media.MIME_TYPE} = ?)"
        
        val selectionArgs = arrayOf("audio/mpeg", "audio/wav")
        
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"
        
        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val title = cursor.getString(titleColumn)
                val artist = cursor.getString(artistColumn)
                val album = cursor.getString(albumColumn)
                val duration = cursor.getLong(durationColumn)
                val path = cursor.getString(pathColumn)
                val albumId = cursor.getLong(albumIdColumn)
                
                if (duration > 0) {
                    songs.add(Song(id, title, artist, album, duration, path, albumId))
                }
            }
        }
        
        return@withContext songs
    }
}

class MusicViewModel(private val repository: MusicRepository) : ViewModel() {
    val songs = mutableStateOf<List<Song>>(emptyList())
    val isLoading = mutableStateOf(false)
    
    suspend fun loadMusic() {
        isLoading.value = true
        try {
            songs.value = repository.loadSongs()
        } finally {
            isLoading.value = false
        }
    }
}

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "Permiso concedido", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Se necesita permiso para acceder a la música", Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_MEDIA_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                // Permiso ya concedido
                setContent {
                    AndroOsPlayerApp()
                }
            } else {
                requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_AUDIO)
            }
        } else {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                setContent {
                    AndroOsPlayerApp()
                }
            } else {
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AndroOsPlayerApp() {
    val context = LocalContext.current
    val viewModel: MusicViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MusicViewModel(MusicRepository(context)) as T
            }
        }
    )
    
    val songs by viewModel.songs.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadMusic()
    }
    
    MaterialTheme(
        colorScheme = darkColorScheme()
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF0A0A0A)
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "ANDRO_OS PLAYER",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = Color(0xFF00FFAA)
                            )
                        },
                        colors = topAppBarColors()
                    )
                }
            ) { paddingValues ->
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF00FFAA))
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        // Lista de canciones
                        LazyColumn(
                            modifier = Modifier.weight(1f)
                        ) {
                            items(songs) { song ->
                                SongItem(song = song)
                            }
                        }
                        
                        // Controles y efectos
                        PlayerControls()
                        AudioEffectsPanel()
                    }
                }
            }
        }
    }
}

@Composable
fun SongItem(song: Song) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        colors = cardColors()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF00FFAA), Color(0xFF0088FF))
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = android.R.drawable.ic_media_play),
                    contentDescription = "Play",
                    tint = Color.Black,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = song.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "${song.artist} • ${song.album}",
                    color = Color(0xFFAAAAAA),
                    fontSize = 12.sp
                )
            }
            
            IconButton(
                onClick = { /* Play song */ },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    painter = painterResource(id = android.R.drawable.ic_media_play),
                    contentDescription = "Play",
                    tint = Color(0xFF00FFAA)
                )
            }
        }
    }
}

@Composable
fun PlayerControls() {
    var isPlaying by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = cardColors()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "CONTROLES",
                color = Color(0xFF00FFAA),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(
                    onClick = { /* Previous */ },
                    modifier = Modifier.size(50.dp)
                ) {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_media_previous),
                        contentDescription = "Previous",
                        tint = Color(0xFF00FFAA),
                        modifier = Modifier.size(30.dp)
                    )
                }
                
                IconButton(
                    onClick = { isPlaying = !isPlaying },
                    modifier = Modifier.size(70.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(
                                color = Color(0xFF00FFAA),
                                shape = RoundedCornerShape(30.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = if (isPlaying) 
                                android.R.drawable.ic_media_pause 
                            else 
                                android.R.drawable.ic_media_play),
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.Black,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
                
                IconButton(
                    onClick = { /* Next */ },
                    modifier = Modifier.size(50.dp)
                ) {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_media_next),
                        contentDescription = "Next",
                        tint = Color(0xFF00FFAA),
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AudioEffectsPanel() {
    var masterGain by remember { mutableFloatStateOf(1000f) }
    var bassBoost by remember { mutableFloatStateOf(0f) }
    val eqBands = remember { mutableListOf(0f, 0f, 0f, 0f, 0f) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = cardColors()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "EFECTOS DE AUDIO",
                color = Color(0xFF00FFAA),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Master Gain
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "GANANCIA MASTER",
                        color = Color.White,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "${masterGain.toInt()} mB",
                        color = Color(0xFF00FFAA),
                        fontSize = 12.sp
                    )
                }
                Slider(
                    value = masterGain,
                    onValueChange = { masterGain = it },
                    valueRange = 0f..2000f,
                    modifier = Modifier.fillMaxWidth(),
                    colors = sliderColors()
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Bass Boost
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "BASS BOOST",
                        color = Color.White,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "${bassBoost.toInt()}/1000",
                        color = Color(0xFF00FFAA),
                        fontSize = 12.sp
                    )
                }
                Slider(
                    value = bassBoost,
                    onValueChange = { bassBoost = it },
                    valueRange = 0f..1000f,
                    modifier = Modifier.fillMaxWidth(),
                    colors = sliderColors()
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Equalizer
            Text(
                text = "ECUALIZADOR",
                color = Color.White,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            AudioEffectsController.EQ_FREQUENCIES.forEachIndexed { index, freq ->
                Column(
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = freq,
                            color = Color(0xFFAAAAAA),
                            fontSize = 10.sp
                        )
                        Text(
                            text = "${(eqBands[index] * 1000).toInt()} mB",
                            color = Color(0xFF00FFAA),
                            fontSize = 10.sp
                        )
                    }
                    Slider(
                        value = eqBands[index],
                        onValueChange = { eqBands[index] = it },
                        valueRange = -1f..1f,
                        modifier = Modifier.fillMaxWidth(),
                        colors = sliderColors()
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Botón de reset
            Button(
                onClick = {
                    masterGain = 1000f
                    bassBoost = 0f
                    eqBands.indices.forEach { eqBands[it] = 0f }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = buttonColors()
            ) {
                Text(
                    text = "RESET A VALORES POR DEFECTO",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun darkColorScheme() = androidx.compose.material3.darkColorScheme(
    primary = Color(0xFF00FFAA),
    secondary = Color(0xFF0088FF),
    tertiary = Color(0xFFFF0088),
    background = Color(0xFF0A0A0A),
    surface = Color(0xFF1A1A1A),
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun topAppBarColors() = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
    containerColor = Color(0xFF0A0A0A),
    titleContentColor = Color(0xFF00FFAA),
    actionIconContentColor = Color(0xFF00FFAA)
)

@Composable
fun cardColors() = androidx.compose.material3.CardDefaults.cardColors(
    containerColor = Color(0xFF1A1A1A),
    contentColor = Color.White
)

@Composable
fun sliderColors() = androidx.compose.material3.SliderDefaults.colors(
    thumbColor = Color(0xFF00FFAA),
    activeTrackColor = Color(0xFF00FFAA),
    inactiveTrackColor = Color(0xFF333333)
)

@Composable
fun buttonColors() = androidx.compose.material3.ButtonDefaults.buttonColors(
    containerColor = Color(0xFF00FFAA),
    contentColor = Color.Black
)
