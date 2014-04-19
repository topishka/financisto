/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 ******************************************************************************/
package ru.orangesoftware.financisto.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;

import java.text.DecimalFormatSymbols;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.db.MyEntityManager;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.model.SymbolFormat;
import ru.orangesoftware.financisto.utils.CurrencyCache;
import ru.orangesoftware.financisto.utils.PinProtection;

import static ru.orangesoftware.financisto.utils.Utils.checkEditText;
import static ru.orangesoftware.financisto.utils.Utils.text;

public class CurrencyActivity extends Activity {
	
	public static final String CURRENCY_ID_EXTRA = "currencyId";
	private static final DecimalFormatSymbols s = new DecimalFormatSymbols();
	
	private DatabaseAdapter db;
	private MyEntityManager em;
	
	private String[] decimalSeparatorsItems;
	private String[] groupSeparatorsItems;
    private SymbolFormat[] symbolFormats;

    private EditText name;
    private EditText title;
    private EditText symbol;
    private CheckBox isDefault;
	private Spinner decimals;
    private Spinner decimalSeparators;
    private Spinner groupSeparators;
    private Spinner symbolFormat;

	private int maxDecimals; 

	private Currency currency = new Currency();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_LEFT_ICON);
		setContentView(R.layout.currency);
		setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.ic_dialog_currency);

		db = new DatabaseAdapter(this);
		em = db.em();

        name = (EditText)findViewById(R.id.name);
        title = (EditText)findViewById(R.id.title);
        symbol = (EditText)findViewById(R.id.symbol);
        isDefault = (CheckBox)findViewById(R.id.is_default);
		decimals = (Spinner)findViewById(R.id.spinnerDecimals);
		decimalSeparators = (Spinner)findViewById(R.id.spinnerDecimalSeparators);
		groupSeparators = (Spinner)findViewById(R.id.spinnerGroupSeparators);
		groupSeparators.setSelection(1);
        symbolFormat = (Spinner)findViewById(R.id.spinnerSymbolFormat);
        symbolFormat.setSelection(0);

		maxDecimals = decimals.getCount()-1;
		
		decimalSeparatorsItems = getResources().getStringArray(R.array.decimal_separators);
		groupSeparatorsItems = getResources().getStringArray(R.array.group_separators);
        symbolFormats = SymbolFormat.values();

		Button bOk = (Button)findViewById(R.id.bOK);
		bOk.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View view) {
				if (checkEditText(title, "title", true, 100)
						&& checkEditText(name, "code", true, 3)
						&& checkEditText(symbol, "symbol", true, 3)) {
					currency.title = text(title);
					currency.name = text(name);
					currency.symbol = text(symbol);
					currency.isDefault = isDefault.isChecked();
					currency.decimals = maxDecimals-decimals.getSelectedItemPosition();
					currency.decimalSeparator = decimalSeparators.getSelectedItem().toString();
					currency.groupSeparator = groupSeparators.getSelectedItem().toString();
                    currency.symbolFormat = symbolFormats[symbolFormat.getSelectedItemPosition()];
					long id = em.saveOrUpdate(currency);
					CurrencyCache.initialize(em);
					Intent data = new Intent();
					data.putExtra(CURRENCY_ID_EXTRA, id);
					setResult(RESULT_OK, data);
					finish();
				}
			}		
		});

		Button bCancel = (Button)findViewById(R.id.bCancel);
		bCancel.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View view) {
				setResult(RESULT_CANCELED, null);
				finish();
			}		
		});

		Intent intent = getIntent();
		if (intent != null) {
			long id = intent.getLongExtra(CURRENCY_ID_EXTRA, -1);
			if (id != -1) {
				currency = em.load(Currency.class, id);
				editCurrency();
			} else {
                makeDefaultIfNecessary();
            }
		}
	}

    private void makeDefaultIfNecessary() {
        isDefault.setChecked(em.getAllCurrenciesList().isEmpty());
    }

    private void editCurrency() {
		Currency currency = this.currency;
		EditText name = (EditText)findViewById(R.id.name);
		name.setText(currency.name);
		EditText title = (EditText)findViewById(R.id.title);
		title.setText(currency.title);
		EditText symbol = (EditText)findViewById(R.id.symbol);
		symbol.setText(currency.symbol);
		CheckBox isDefault = (CheckBox)findViewById(R.id.is_default);
		isDefault.setChecked(currency.isDefault);
		decimals.setSelection(maxDecimals-currency.decimals);
		decimalSeparators.setSelection(indexOf(decimalSeparatorsItems, currency.decimalSeparator, s.getDecimalSeparator()));
		groupSeparators.setSelection(indexOf(groupSeparatorsItems, currency.groupSeparator, s.getGroupingSeparator()));
        symbolFormat.setSelection(currency.symbolFormat.ordinal());
	}

	private int indexOf(String[] a, String v, char c) {
		int count = a.length;
		int d = -1;
		for (int i=0; i<count; i++) {
			String s = a[i];
			if (v != null && s.charAt(1) == v.charAt(1)) {
				return i;
			} 
			if (s.charAt(1) == c) {
				d = i;
			}
		}
		return d;
	}

	@Override
	protected void onPause() {
		super.onPause();
		PinProtection.lock(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		PinProtection.unlock(this);
	}
}
