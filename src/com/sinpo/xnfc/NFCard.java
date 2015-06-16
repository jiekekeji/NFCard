/* NFCard is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 3 of the License, or
(at your option) any later version.

NFCard is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with Wget.  If not, see <http://www.gnu.org/licenses/>.

Additional permission under GNU GPL version 3 section 7 */

package com.sinpo.xnfc;

import java.io.IOException;

import org.xml.sax.XMLReader;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.ClipboardManager;
import android.text.Editable;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;

import com.sinpo.xnfc.card.CardManager;

@SuppressLint("NewApi")
public final class NFCard extends Activity implements OnClickListener,
		Html.ImageGetter, Html.TagHandler {
	// 支付系统文件
	private final static byte[] DFN_SRV = { (byte) 'P', (byte) 'A', (byte) 'Y',
			(byte) '.', (byte) 'S', (byte) 'Z', (byte) 'T' };

	private final static byte[] SELECT = { (byte) 0x00, (byte) 0xA4,
			(byte) 0x04, (byte) 0x00, (byte) 0x0E, (byte) 0x31, (byte) 0x50,
			(byte) 0x41, (byte) 0x59, (byte) 0x2E, (byte) 0x53, (byte) 0x59,
			(byte) 0x53, (byte) 0x2E, (byte) 0x44, (byte) 0x44, (byte) 0x46,
			(byte) 0x30, (byte) 0x31 };

	private final static byte[] RESPONSE = { (byte) 0x00, (byte) 0xC0,
			(byte) 0x00, (byte) 0x00, (byte) 0x20 };

	private final static byte[] reset = { (byte) 0x00, (byte) 0xA4,
			(byte) 0x04, (byte) 0x00, (byte) 0x0E, (byte) 0x32, (byte) 0x50,
			(byte) 0x41, (byte) 0x59, (byte) 0x2E, (byte) 0x53, (byte) 0x59,
			(byte) 0x53, (byte) 0x2E, (byte) 0x44, (byte) 0x44, (byte) 0x46,
			(byte) 0x30, (byte) 0x31 };

	public static final short SW_NO_ERROR = (short) 0x9000;

	private NfcAdapter nfcAdapter;
	private PendingIntent pendingIntent;
	private Resources res;
	private TextView board;

	private enum ContentType {
		HINT, DATA, MSG
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.nfcard);

		final Resources res = getResources();
		this.res = res;

		final View decor = getWindow().getDecorView();
		final TextView board = (TextView) decor.findViewById(R.id.board);
		this.board = board;

		decor.findViewById(R.id.btnCopy).setOnClickListener(this);
		decor.findViewById(R.id.btnNfc).setOnClickListener(this);
		decor.findViewById(R.id.btnExit).setOnClickListener(this);

		board.setMovementMethod(LinkMovementMethod.getInstance());
		board.setFocusable(false);
		board.setClickable(false);
		board.setLongClickable(false);

		nfcAdapter = NfcAdapter.getDefaultAdapter(this);
		pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this,
				getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.clear:
			showData(null);
			return true;
		case R.id.help:
			showHelp(R.string.info_help);
			return true;
		case R.id.about:
			showHelp(R.string.info_about);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();

		if (nfcAdapter != null)
			nfcAdapter.disableForegroundDispatch(this);
	}

	@Override
	protected void onResume() {
		super.onResume();

		if (nfcAdapter != null)
			nfcAdapter.enableForegroundDispatch(this, pendingIntent,
					CardManager.FILTERS, CardManager.TECHLISTS);

		refreshStatus();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);

		final Parcelable p = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
		Log.d("NFCTAG", intent.getAction());
		final Tag tag = (Tag) p;

		String[] techs = tag.getTechList();
		for (String string : techs) {
			Log.i("tech", string);
		}

		final IsoDep isodep = IsoDep.get(tag);

		try {
			isodep.connect();

			byte[] name = cmd(reset, isodep);
			for (byte b : name) {
				Log.i("name", b + "");
			}

			if (isOkey(name)) {
				Log.i("name", "is ok");
				byte[] response = cmd(RESPONSE, isodep);
				for (byte b : response) {
					Log.i("response", b + "");
				}
				if (isOkey(response)) {
					Log.i("respinse", "is ok");
				}

			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/**
	 * 选择支付系统文件
	 * 
	 * @param name
	 * @param isoDep
	 * @return
	 */
	public byte[] cmd(byte[] apdu, IsoDep isodep) {
		try {
			return isodep.transceive(apdu);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	/***
	 * 验证指令是否发送成功
	 * 
	 * @param data
	 * @return
	 */
	public boolean isOkey(byte[] data) {
		final byte[] d = data;
		int n = d.length;
		short s = (short) ((d[n - 2] << 8) | (0xFF & d[n - 1]));

		return SW_NO_ERROR == s;
	}

	@Override
	public void onClick(final View v) {
		switch (v.getId()) {
		case R.id.btnCopy: {
			copyData();
			break;
		}
		case R.id.btnNfc: {
			startActivityForResult(new Intent(
					android.provider.Settings.ACTION_WIRELESS_SETTINGS), 0);
			break;
		}
		case R.id.btnExit: {
			finish();
			break;
		}
		default:
			break;
		}
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		refreshStatus();
	}

	private void refreshStatus() {
		final Resources r = this.res;

		final String tip;
		if (nfcAdapter == null)
			tip = r.getString(R.string.tip_nfc_notfound);
		else if (nfcAdapter.isEnabled())
			tip = r.getString(R.string.tip_nfc_enabled);
		else
			tip = r.getString(R.string.tip_nfc_disabled);

		final StringBuilder s = new StringBuilder(
				r.getString(R.string.app_name));

		s.append("  --  ").append(tip);
		setTitle(s);

		final CharSequence text = board.getText();
		if (text == null || board.getTag() == ContentType.HINT)
			showHint();
	}

	private void copyData() {
		final CharSequence text = board.getText();
		if (text == null || board.getTag() != ContentType.DATA)
			return;

		((ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE))
				.setText(text);

		final String msg = res.getString(R.string.msg_copied);
		final Toast toast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
		toast.setGravity(Gravity.CENTER, 0, 0);
		toast.show();
	}

	private void showData(String data) {
		if (data == null || data.length() == 0) {
			showHint();
			return;
		}

		final TextView board = this.board;
		final Resources res = this.res;

		final int padding = res.getDimensionPixelSize(R.dimen.pnl_margin);

		board.setPadding(padding, padding, padding, padding);
		board.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
		board.setTextSize(res.getDimension(R.dimen.text_small));
		board.setTextColor(res.getColor(R.color.text_default));
		board.setGravity(Gravity.NO_GRAVITY);
		board.setTag(ContentType.DATA);
		board.setText(Html.fromHtml(data));
	}

	private void showHelp(int id) {
		final TextView board = this.board;
		final Resources res = this.res;

		final int padding = res.getDimensionPixelSize(R.dimen.pnl_margin);

		board.setPadding(padding, padding, padding, padding);
		board.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
		board.setTextSize(res.getDimension(R.dimen.text_small));
		board.setTextColor(res.getColor(R.color.text_default));
		board.setGravity(Gravity.NO_GRAVITY);
		board.setTag(ContentType.MSG);
		board.setText(Html.fromHtml(res.getString(id), this, this));
	}

	private void showHint() {
		final TextView board = this.board;
		final Resources res = this.res;
		final String hint;

		if (nfcAdapter == null)
			hint = res.getString(R.string.msg_nonfc);
		else if (nfcAdapter.isEnabled())
			hint = res.getString(R.string.msg_nocard);
		else
			hint = res.getString(R.string.msg_nfcdisabled);

		final int padding = res.getDimensionPixelSize(R.dimen.text_middle);

		board.setPadding(padding, padding, padding, padding);
		board.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD_ITALIC));
		board.setTextSize(res.getDimension(R.dimen.text_middle));
		board.setTextColor(res.getColor(R.color.text_tip));
		board.setGravity(Gravity.CENTER_VERTICAL);
		board.setTag(ContentType.HINT);
		board.setText(Html.fromHtml(hint));
	}

	@Override
	public void handleTag(boolean opening, String tag, Editable output,
			XMLReader xmlReader) {
		if (!opening && "version".equals(tag)) {
			try {
				output.append(getPackageManager().getPackageInfo(
						getPackageName(), 0).versionName);
			} catch (NameNotFoundException e) {
			}
		}
	}

	@Override
	public Drawable getDrawable(String source) {
		final Resources r = getResources();

		final Drawable ret;
		final String[] params = source.split(",");
		if ("icon_main".equals(params[0])) {
			ret = r.getDrawable(R.drawable.ic_app_main);
		} else {
			ret = null;
		}

		if (ret != null) {
			final float f = r.getDisplayMetrics().densityDpi / 72f;
			final int w = (int) (Util.parseInt(params[1], 10, 16) * f + 0.5f);
			final int h = (int) (Util.parseInt(params[2], 10, 16) * f + 0.5f);
			ret.setBounds(0, 0, w, h);
		}

		return ret;
	}
}
