package per.goweii.demo.reveallayout;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.reveal_layout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("RevealLayout", "onClick1");
            }
        });
        findViewById(R.id.reveal_layout2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("RevealLayout", "onClick2");
            }
        });
    }
}
