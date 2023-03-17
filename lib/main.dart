
import 'package:dynamic_color/dynamic_color.dart';
import 'package:flutter/material.dart';
import 'package:flutter/scheduler.dart';
import 'package:flutter/services.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:shared_preferences/shared_preferences.dart';


late SharedPreferences sharedPreferences;
late MethodChannel methodChannel;

void main() async {
  runApp(const App());
}

class App extends StatelessWidget {
  const App({super.key});

  @override
  Widget build(BuildContext context) {
    return DynamicColorBuilder(
      builder: (ColorScheme? light, ColorScheme? dark) {
        bool isDark = SchedulerBinding.instance.window.platformBrightness == Brightness.dark;

        // Get theme data with monet
        var themeData = ThemeData(useMaterial3: true);
        if (isDark) {
          themeData = ThemeData.dark(useMaterial3: true);
          if (dark != null) {
            themeData = themeData.copyWith(colorScheme: dark);
          }
        } else {
          if (light != null) {
            themeData = themeData.copyWith(colorScheme: light);
          }
        }

        return MaterialApp(
            theme: themeData,
            home: const LoadingScreen()
        );
      }
    );
  }
}

class LoadingScreen extends StatelessWidget {
  const LoadingScreen({Key? key}) : super(key: key);

  // Initialize platform channels
  Future<void> init() async {
    sharedPreferences = await SharedPreferences.getInstance();
    methodChannel = const MethodChannel("eu.marekkon5.album_art_background/native");
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text("Album Art Background"),
      ),
      body: FutureBuilder(
        future: init(),
        builder: (context, snapshot) {
          if (snapshot.connectionState == ConnectionState.done) {
            return const SettingsScreen();
          }
          return const Center(child: CircularProgressIndicator());
        },
      )
    );
  }
}


class SettingsScreen extends StatefulWidget {
  const SettingsScreen({super.key});

  @override
  State<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends State<SettingsScreen> {

  double blurStrength = 25.0;

  @override
  void initState() {
    blurStrength = sharedPreferences.getDouble("blurStrength")??25.0;
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return ListView(
      children: [
        ListTile(
          title: const Text("Enable"),
          leading: const Icon(Icons.done),
          trailing: Switch(
            value: sharedPreferences.getBool("enable")??false,
            onChanged: (bool v) {
              sharedPreferences.setBool("enable", v);
              setState(() {});
            },
          ),
        ),

        const Divider(),

        ListTile(
          title: const Text("Open notification permission settings"),
          subtitle: const Text("Notification permissions are required to read the playback state notification"),
          leading: const Icon(Icons.notifications),
          onTap: () async {
            await methodChannel.invokeMethod("notificationAccessSettings");
          }
        ),
        ListTile(
          title: const Text("Request storage permission"),
          subtitle: const Text("Required to change & backup wallpapers"),
          leading: const Icon(Icons.sd_storage),
          onTap: () async {
            await Permission.storage.request();
          }
        ),
        ListTile(
          title: const Text("Open battery optimization settings"),
          subtitle: const Text("Required on Android 13+ to work properly"),
          leading: const Icon(Icons.battery_full),
          onTap: () async {
            await methodChannel.invokeMethod("batteryOptimizationSettings");
          }
        ),

        const Divider(),

        ListTile(
          title: const Text("Blur"),
          subtitle: const Text("Blur the album art background"),
          leading: const Icon(Icons.blur_circular),
          trailing: Switch(
            value: sharedPreferences.getBool("blur")??false,
            onChanged: (bool v) {
              sharedPreferences.setBool("blur", v);
              setState(() {});
            },
          ),
        ),
        if (sharedPreferences.getBool("blur")??false)
        ListTile(
          title: Text("Blur strength: ${blurStrength.round()}"),
          leading: const Icon(Icons.blur_linear),
          subtitle: Slider(
            min: 1.0,
            max: 50.0,
            value: blurStrength,
            onChanged: (double value) {
              setState(() {
                blurStrength = value;
              });
            },
            onChangeEnd: (double value) {
              sharedPreferences.setDouble("blurStrength", value.roundToDouble());
            },
          ),
        ),
        ListTile(
          title: const Text("Homescreen wallpaper"),
          subtitle: const Text("Change homescreen wallpaper as well"),
          leading: const Icon(Icons.home),
          trailing: Switch(
            value: sharedPreferences.getBool("homescreen")??false,
            onChanged: (bool v) {
              sharedPreferences.setBool("homescreen", v);
              setState(() {});
            },
          ),
        ),

        const Divider(),

        ListTile(
          title: const Text("Restore backed-up wallpaper"),
          subtitle: const Text("In case something goes wrong..."),
          leading: const Icon(Icons.settings_backup_restore),
          onTap: () async {
            await methodChannel.invokeMethod("restoreBackup");
          },
        )
      ]
    );
  }
}