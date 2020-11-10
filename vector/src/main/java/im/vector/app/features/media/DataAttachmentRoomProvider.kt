/*
 * Copyright (c) 2020 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.features.media

import im.vector.app.core.date.VectorDateFormatter
import im.vector.app.core.resources.StringProvider
import im.vector.lib.attachmentviewer.AttachmentInfo
import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.session.file.FileService
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import java.io.File

class DataAttachmentRoomProvider(
        private val attachments: List<AttachmentData>,
        private val room: Room?,
        imageContentRenderer: ImageContentRenderer,
        dateFormatter: VectorDateFormatter,
        fileService: FileService,
        stringProvider: StringProvider
) : BaseAttachmentProvider(imageContentRenderer, fileService, dateFormatter, stringProvider) {

    override fun getItemCount(): Int = attachments.size

    override fun getAttachmentInfoAt(position: Int): AttachmentInfo {
        return attachments[position].let {
            when (it) {
                is ImageContentRenderer.Data -> {
                    if (it.mimeType == "image/gif") {
                        AttachmentInfo.AnimatedImage(
                                uid = it.eventId,
                                url = it.url ?: "",
                                data = it
                        )
                    } else {
                        AttachmentInfo.Image(
                                uid = it.eventId,
                                url = it.url ?: "",
                                data = it
                        )
                    }
                }
                is VideoContentRenderer.Data -> {
                    AttachmentInfo.Video(
                            uid = it.eventId,
                            url = it.url ?: "",
                            data = it,
                            thumbnail = AttachmentInfo.Image(
                                    uid = it.eventId,
                                    url = it.thumbnailMediaData.url ?: "",
                                    data = it.thumbnailMediaData
                            )
                    )
                }
                else                         -> throw IllegalArgumentException()
            }
        }
    }

    override fun getTimelineEventAtPosition(position: Int): TimelineEvent? {
        val item = attachments[position]
        return room?.getTimeLineEvent(item.eventId)
    }

    override fun getFileForSharing(position: Int, callback: (File?) -> Unit) {
        val item = attachments[position]
        fileService.downloadFile(
                downloadMode = FileService.DownloadMode.FOR_EXTERNAL_SHARE,
                id = item.eventId,
                fileName = item.filename,
                mimeType = item.mimeType,
                url = item.url ?: "",
                elementToDecrypt = item.elementToDecrypt,
                callback = object : MatrixCallback<File> {
                    override fun onSuccess(data: File) {
                        callback(data)
                    }

                    override fun onFailure(failure: Throwable) {
                        callback(null)
                    }
                }
        )
    }
}
