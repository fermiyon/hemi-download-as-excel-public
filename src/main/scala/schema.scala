// Generated by <a href="http://scalaxb.org/">scalaxb</a>.
package com.selmank


case class RECORD(SUBE_KODU: Int,
  REFNO: Int,
  STOK_KODU: String,
  STOK_ADI: String,
  ALISFIYATI: String,
  P_KDV: Byte,
  SATISFIYATI1: Float,
  BARKOD: String,
  BARKOD2: String,
  BARKOD3: String,
  URETIM_YERI_NO: String,
  FIYAT_DEGISIM_TARIHI: String,
  BIRIM2: String,
  BIRIM2SATISFIYATI: String,
  BIRIM2BARKODU: String,
  BAKIYE: Float)


case class LIST(RECORD: RECORD*)
