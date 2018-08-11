package com.example.android.shoppinglistapp;

import android.Manifest;
import android.app.Activity;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NavUtils;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import static com.example.android.shoppinglistapp.data.DataContract.ProductEntry.COLUMN_PRODUCT_IMAGE;
import static com.example.android.shoppinglistapp.data.DataContract.ProductEntry.COLUMN_PRODUCT_NAME;
import static com.example.android.shoppinglistapp.data.DataContract.ProductEntry.COLUMN_PRODUCT_PRICE;
import static com.example.android.shoppinglistapp.data.DataContract.ProductEntry.COLUMN_PRODUCT_QUANTITY;
import static com.example.android.shoppinglistapp.data.DataContract.ProductEntry.CONTENT_URI;
import static com.example.android.shoppinglistapp.data.DataContract.ProductEntry._ID;

public class EditActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int PRODUCT_LOADER = 0;
    private static final int MAX_PRODUCT_QUANTITY = 50;
    private static final int MIN_PRODUCT_QUANTITY = 0;
    String quantityString;
    Uri currentItemUri;
    Uri imageUri;
    private boolean productHasChanged = false;
    private EditText mProductName;
    private EditText mProductPrice;
    private EditText mProductQuantity;
    private Button mDecreaseButton;
    private Button mIncreaseButton;
    private Button mOrderButton;
    private Button mUploadButton;
    private ImageView mProductImage;
    private Button mSaveButton;
    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            productHasChanged = true;
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);
        Intent intent = getIntent();
        currentItemUri = intent.getData();
        if (currentItemUri == null) {
            setTitle(R.string.editor_activity_title_add_item);
        } else {
            setTitle(R.string.editor_activity_title_edit_product);
            getLoaderManager().initLoader(PRODUCT_LOADER, null, this);
        }
        mProductImage = (ImageView) findViewById(R.id.product_image);
        mProductName = (EditText) findViewById(R.id.edit_product_name);
        mProductPrice = (EditText) findViewById(R.id.edit_price);
        mProductQuantity = (EditText) findViewById(R.id.product_quantity);
        mDecreaseButton = (Button) findViewById(R.id.decrement);
        mIncreaseButton = (Button) findViewById(R.id.increment);
        mUploadButton = (Button) findViewById(R.id.uploadButton);
        mOrderButton = (Button) findViewById(R.id.order_button);
        mSaveButton = (Button) findViewById(R.id.save_button_and_store);
        mProductName.setOnTouchListener(mTouchListener);
        mProductPrice.setOnTouchListener(mTouchListener);
        mProductQuantity.setOnTouchListener(mTouchListener);
        mDecreaseButton.setOnTouchListener(mTouchListener);
        mIncreaseButton.setOnTouchListener(mTouchListener);
        mOrderButton.setOnTouchListener(mTouchListener);
        mUploadButton.setOnTouchListener(mTouchListener);
        mSaveButton.setOnTouchListener(mTouchListener);
        mUploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                trySelector();
                productHasChanged = true;
            }
        });

      mIncreaseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int quantity = Integer.parseInt(mProductQuantity.getText().toString().trim());
                if (quantity == MAX_PRODUCT_QUANTITY) {
                    Toast.makeText(getApplicationContext(), getString(R.string.increment_toast),
                            Toast.LENGTH_SHORT).show();
                } else {
                    quantity = quantity + 1;
                    mProductQuantity.setText(Integer.toString(quantity));
                }
            }
        });

        mDecreaseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int quantity = Integer.parseInt(mProductQuantity.getText().toString().trim());
                if (quantity == MIN_PRODUCT_QUANTITY) {
                    Toast.makeText(getApplicationContext(), getString(R.string.decrement_toast),
                            Toast.LENGTH_SHORT).show();
                } else {
                    quantity = quantity - 1;
                    mProductQuantity.setText(Integer.toString(quantity));
                }
            }
        });

        mOrderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String productNameMail = mProductName.getText().toString().trim();
                String productQuantitytMail = mProductQuantity.getText().toString().trim();
                String productPriceMail = mProductPrice.getText().toString().trim();
                String message = getString(R.string.email_message) +
                        "\nProductName: " + productNameMail +
                        "\nQuantityt: " + productQuantitytMail +
                        "\nPrice per item (EUR): " + productPriceMail +
                        "\nThank you." +
                        "Best regards,";
                Intent intent = new Intent(Intent.ACTION_SENDTO);
                intent.setData(Uri.parse("mailto:"));
                intent.putExtra(Intent.EXTRA_SUBJECT,
                        getString(R.string.email_subject));
                intent.putExtra(Intent.EXTRA_TEXT, message);

                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                }
            }
        });

        mSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean productSaved = saveProduct();
                if (productSaved) finish();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        if (currentItemUri == null) {
            MenuItem deleteItem = menu.findItem(R.id.action_delete);
            deleteItem.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (currentItemUri == null) {
            MenuItem deleteItem = menu.findItem(R.id.action_delete);
            deleteItem.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_delete:
                showDeleteConfirmationDialog();
                return true;
            case android.R.id.home:
                if (!productHasChanged) {
                    NavUtils.navigateUpFromSameTask(EditActivity.this);
                    return true;
                }
                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                NavUtils.navigateUpFromSameTask(EditActivity.this);
                            }
                        };
                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (!productHasChanged) {
            super.onBackPressed();
            return;
        }
        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                };
        showUnsavedChangesDialog(discardButtonClickListener);
    }


    private boolean saveProduct() {
        String name = mProductName.getText().toString().trim();
        String price = mProductPrice.getText().toString().trim();
        quantityString = mProductQuantity.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            Toast.makeText(this, getString(R.string.toast_require_name), Toast.LENGTH_SHORT).show();
            return false;
        }
        ContentValues values = new ContentValues();
        values.put(COLUMN_PRODUCT_NAME, name);

        int quantity = !TextUtils.isEmpty(quantityString) ? Integer.parseInt(quantityString) : 0;
        values.put(COLUMN_PRODUCT_QUANTITY, quantity);

        if (TextUtils.isEmpty(price)) {
            Toast.makeText(this, getString(R.string.toast_require_price), Toast.LENGTH_SHORT).show();
            return false;
        }

        values.put(COLUMN_PRODUCT_PRICE, price);

        if (imageUri == null) {
            Toast.makeText(this, getString(R.string.toast_required_image), Toast.LENGTH_SHORT).show();
            return false;
        }

        String image = imageUri.toString();
        values.put(COLUMN_PRODUCT_IMAGE, image);

        if (currentItemUri == null) {
            setTitle("Add Product");
            supportInvalidateOptionsMenu();
            Uri newUri = getContentResolver().insert(CONTENT_URI, values);
            if (newUri == null) {
                Toast.makeText(this, getString(R.string.editor_insert_failed),
                        Toast.LENGTH_SHORT).show();
                return false;
            } else {
                Toast.makeText(this, getString(R.string.editor_insert_product_successful),
                        Toast.LENGTH_SHORT).show();
                return true;
            }
        } else {
            int rowsAffected = getContentResolver().update(currentItemUri, values, null, null);
            if (rowsAffected == 0) {
                Toast.makeText(this, getString(R.string.editor_insert_failed),
                        Toast.LENGTH_SHORT).show();
                return false;
            } else {
                Toast.makeText(this, getString(R.string.editor_insert_product_successful),
                        Toast.LENGTH_SHORT).show();
                return true;
            }
        }
    }

    private void showToastMessage(int messageResourceId) {
        Toast.makeText(EditActivity.this, getString(messageResourceId),
                Toast.LENGTH_SHORT).show();
    }

    private void showToastIf(boolean condition, int successStringId, int failureStringId) {
        int messageResourceId = condition ? successStringId : failureStringId;
        showToastMessage(messageResourceId);
    }

    private void showUnsavedChangesDialog(DialogInterface.OnClickListener discardButtonClickListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_message);
        builder.setPositiveButton(R.string.action_delete, discardButtonClickListener);
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void showDeleteConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_message);
        builder.setPositiveButton(R.string.action_delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                deleteProduct();
            }
        });
        builder.setNegativeButton(R.string.cancel, null);
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void deleteProduct() {
        if (null != currentItemUri) {
            int rowsDeleted = getContentResolver().delete(currentItemUri, null, null);
            showToastIf(0 == rowsDeleted, R.string.editor_delete_item_failed,
                    R.string.editor_delete_item_successful);
        }
        finish();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] projection = {
                _ID,
                COLUMN_PRODUCT_NAME,
                COLUMN_PRODUCT_PRICE,
                COLUMN_PRODUCT_QUANTITY,
                COLUMN_PRODUCT_IMAGE};
        return new CursorLoader(this, currentItemUri,
                projection, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (null == cursor || 1 > cursor.getCount()) {
            return;
        }
        if (cursor.moveToFirst()) {
            int nameColumnIndex = cursor.getColumnIndex(COLUMN_PRODUCT_NAME);
            int priceColumnIndex = cursor.getColumnIndex(COLUMN_PRODUCT_PRICE);
            int quantityColumnIndex = cursor.getColumnIndex(COLUMN_PRODUCT_QUANTITY);
            int pictureColumnIndex = cursor.getColumnIndex(COLUMN_PRODUCT_IMAGE);
            String name = cursor.getString(nameColumnIndex);
            String price = cursor.getString(priceColumnIndex);
            int quantity = cursor.getInt(quantityColumnIndex);
            String picture = cursor.getString(pictureColumnIndex);
            mProductName.setText(name);
            mProductPrice.setText(price);
            mProductQuantity.setText(Integer.toString(quantity));
            mProductImage.setImageURI(Uri.parse(picture));
            imageUri = Uri.parse(picture);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mProductName.setText("");
        mProductPrice.setText("");
        mProductQuantity.setText("");
    }


    public void trySelector() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            return;
        }
        openSelector();
    }

    private void openSelector() {
        Intent intent;
        if (Build.VERSION.SDK_INT < 19) {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
        } else {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
        }
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select picture"), 0);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openSelector();
                }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 0 && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                imageUri = data.getData();
                mProductImage.setImageURI(imageUri);
                mProductImage.invalidate();
            }
        }
    }
}