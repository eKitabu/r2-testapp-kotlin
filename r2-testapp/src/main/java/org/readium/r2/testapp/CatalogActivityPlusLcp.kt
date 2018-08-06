/*
 * Module: r2-testapp-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.testapp

import android.app.ProgressDialog
import android.net.Uri
import android.os.Bundle
import android.widget.EditText
import com.mcxiaoke.koi.HASH
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.task
import nl.komponents.kovenant.then
import nl.komponents.kovenant.ui.successUi
import org.jetbrains.anko.*
import org.jetbrains.anko.appcompat.v7.Appcompat
import org.jetbrains.anko.design.textInputLayout
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.readium.r2.lcp.LcpHttpService
import org.readium.r2.lcp.LcpLicense
import org.readium.r2.lcp.LcpSession
import org.readium.r2.shared.Publication
import org.readium.r2.shared.drm.DRMMModel
import org.readium.r2.shared.drm.Drm
import org.readium.r2.streamer.parser.EpubParser
import org.readium.r2.streamer.parser.PubBox
import java.io.File
import java.net.URL


class CatalogActivityPlusLcp : CatalogActivity(), LcpFunctions {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        listener = this
    }

    override fun parseIntentLcpl(uriString: String) {
        val uri: Uri? = Uri.parse(uriString)
        if (uri != null) {
            val progress = indeterminateProgressDialog(getString(R.string.progress_wait_while_downloading_book))
            progress.show()
            val thread = Thread(Runnable {
                val lcpLicense = LcpLicense(URL(uri.toString()).openStream().readBytes(), this)
                task {
                    lcpLicense.fetchStatusDocument().get()
                } then {
                    lcpLicense.checkStatus()
                    lcpLicense.updateLicenseDocument().get()
                } then {
                    lcpLicense.areRightsValid()
                    lcpLicense.register()
                    lcpLicense.fetchPublication()
                } then {
                    it?.let {
                        lcpLicense.moveLicense(it, URL(uri.toString()).openStream().readBytes())
                    }
                    it!!
                } successUi { path ->
                    val file = File(path)
                    try {
                        runOnUiThread({
                            val parser = EpubParser()
                            val pub = parser.parse(path)
                            if (pub != null) {
                                val pair = parser.parseRemainingResource(pub.container, pub.publication, pub.container.drm)
                                pub.container = pair.first
                                pub.publication = pair.second
                                prepareToServe(pub, file.name, file.absolutePath, true, true)
                                progress.dismiss()

                            }
                        })
                    } catch (e: Throwable) {
                        e.printStackTrace()
                    }
                }
            })
            thread.start()
        }
    }

    override fun prepareAndStartActivityWithLCP(drm: Drm, pub: PubBox, book: Book, file: File, publicationPath: String, parser: EpubParser, publication: Publication) {
        if (drm.brand == Drm.Brand.Lcp) {
            prepareToServe(pub, book.fileName, file.absolutePath, false, true)

            handleLcpPublication(publicationPath, drm, {
                val pair = parser.parseRemainingResource(pub.container, publication, it)
                pub.container = pair.first
                pub.publication = pair.second
            }, {
                if (supportedProfiles.contains(it.profile)) {
                    server.addEpub(publication, pub.container, "/" + book.fileName, applicationContext.getExternalFilesDir(null).path + "/styles/UserProperties.json")

                    val license = (drm.license as LcpLicense)
                    val drmModel = DRMMModel(drm.brand.name,
                            license.currentStatus(),
                            license.provider().toString(),
                            DateTime(license.issued()).toString(DateTimeFormat.shortDateTime()),
                            DateTime(license.lastUpdate()).toString(DateTimeFormat.shortDateTime()),
                            DateTime(license.rightsStart()).toString(DateTimeFormat.shortDateTime()),
                            DateTime(license.rightsEnd()).toString(DateTimeFormat.shortDateTime()),
                            license.rightsPrints().toString(),
                            license.rightsCopies().toString())

                    startActivity(intentFor<R2EpubActivity>("publicationPath" to publicationPath, "epubName" to book.fileName, "publication" to publication, "drmModel" to drmModel))
                } else {
                    alert(Appcompat, "The profile of this DRM is not supported.") {
                        negativeButton("Ok") { }
                    }.show()
                }
            }, {
                // Do nothing
            }).get()

        }
    }

    override fun processLcpActivityResult(uri: Uri, it: Uri, progress: ProgressDialog) {
        val input = contentResolver.openInputStream(uri)

        val thread = Thread(Runnable {
            val lcpLicense = LcpLicense(input.readBytes(), this)
            task {
                lcpLicense.fetchStatusDocument().get()
            } then {
                lcpLicense.checkStatus()
                lcpLicense.updateLicenseDocument().get()
            } then {
                lcpLicense.areRightsValid()
                lcpLicense.register()
                lcpLicense.fetchPublication()
            } then {
                it?.let {
                    lcpLicense.moveLicense(it, contentResolver.openInputStream(uri).readBytes())
                }
                it!!
            } successUi { path ->
                val file = File(path)
                try {
                    runOnUiThread({
                        val parser = EpubParser()
                        val pub = parser.parse(path)
                        if (pub != null) {
                            val pair = parser.parseRemainingResource(pub.container, pub.publication, pub.container.drm)
                            pub.container = pair.first
                            pub.publication = pair.second
                            prepareToServe(pub, file.name, file.absolutePath, true, true)
                            progress.dismiss()
                        }
                    })
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
        })
        thread.start()
    }

    fun handleLcpPublication(publicationPath: String, drm: Drm, parsingCallback: (drm: Drm) -> Unit, callback: (drm: Drm) -> Unit, callbackUI: () -> Unit): Promise<Unit, Exception> {
        val lcpHttpService = LcpHttpService()
        val session = LcpSession(publicationPath, this)

        fun validatePassphrase(passphraseHash: String): Promise<LcpLicense, Exception> {
            return task {
                lcpHttpService.certificateRevocationList("http://crl.edrlab.telesec.de/rl/EDRLab_CA.crl").get()
            } then { pemCrtl ->
                session.resolve(passphraseHash, pemCrtl).get()
            }
        }

        fun promptPassphrase(reason: String? = null, callback: (pass: String) -> Unit) {
            runOnUiThread {
                val hint = session.getHint()
                alert(Appcompat, hint, reason ?: "LCP Passphrase") {
                    var editText: EditText? = null
                    customView {
                        verticalLayout {
                            textInputLayout {
                                editText = editText { }
                            }
                        }
                    }
                    positiveButton("OK") {
                        task {
                            editText!!.text.toString()
                        } then { clearPassphrase ->
                            val passphraseHash = HASH.sha256(clearPassphrase)
                            session.checkPassphrases(listOf(passphraseHash))
                        } then { validPassphraseHash ->
                            session.storePassphrase(validPassphraseHash)
                            callback(validPassphraseHash)
                        }
                    }
                    negativeButton("Cancel") { }
                }.show()
            }
        }

        return task {
            val passphrases = session.passphraseFromDb()
            passphrases?.let {
                val lcpLicense = validatePassphrase(it).get()
                drm.license = lcpLicense
                drm.profile = session.getProfile()
                parsingCallback(drm)
                callback(drm)
            } ?: run {
                promptPassphrase(null, {
                    val lcpLicense = validatePassphrase(it).get()
                    drm.license = lcpLicense
                    drm.profile = session.getProfile()
                    parsingCallback(drm)
                    callback(drm)
                    callbackUI()
                })
            }
        }
    }

}