import SwiftUI
import UniformTypeIdentifiers

struct CampusBackupScreen: View {
    @EnvironmentObject private var session: CampusSession
    @State private var credential = DAVCredential()
    @State private var exporting = false
    @State private var importing = false
    @State private var file: CampusFile?
    @State private var preview: CampusBackup?
    @State private var restoreOwner = ""
    @State private var confirmUpload = false
    @State private var confirmRestore = false
    @State private var busy = false
    @State private var status: String?

    var body: some View {
        Form {
            Section("本机文件") {
                Button("导出全部课表与外观设置") {
                    do { file = try CampusFile(contents()); exporting = true }
                    catch { status = error.localizedDescription }
                }
                Button("从文件恢复") { restoreOwner = session.timetableOwner; importing = true }
            }
            Section("WebDAV") {
                TextField("HTTPS 备份文件地址", text:$credential.address)
                    .textInputAutocapitalization(.never).autocorrectionDisabled().keyboardType(.URL)
                TextField("用户名",text:$credential.user).textInputAutocapitalization(.never).autocorrectionDisabled()
                SecureField("密码或应用专用密码",text:$credential.password)
                Button("保存连接") { do { try credential.save(session.timetableOwner); status = "连接信息已保存" }
                    catch { status = error.localizedDescription } }
                Button("备份到 WebDAV") { confirmUpload = true }
                Button("从 WebDAV 恢复") { transfer(upload:false) }
                Button("移除已保存的连接",role:.destructive) {
                    do { try DAVCredential.remove(session.timetableOwner); credential = DAVCredential(); status = "已移除连接" }
                    catch { status = error.localizedDescription }
                }
            }
            if busy { ProgressView("正在传输") }
            if let status { Text(status).textSelection(.enabled) }
        }
        .disabled(busy)
        .navigationTitle("备份与恢复")
        .task(id:session.timetableOwner) {
            credential = DAVCredential(); preview = nil; status = nil
            do { credential = try DAVCredential.load(session.timetableOwner) ?? DAVCredential() }
            catch { status = error.localizedDescription }
        }
        .fileExporter(isPresented:$exporting,document:file,contentType:.json,defaultFilename:"一校通-iPhone备份.json") { result in
            switch result { case .success: status = "备份文件已保存"
            case .failure(let e): if (e as NSError).code != NSUserCancelledError { status = e.localizedDescription } }
        }
        .fileImporter(isPresented:$importing,allowedContentTypes:[.json,.data]) { result in
            do {
                let url = try result.get()
                guard restoreOwner == session.timetableOwner else { return }
                let scoped = url.startAccessingSecurityScopedResource()
                defer { if scoped { url.stopAccessingSecurityScopedResource() } }
                try offer(Data(contentsOf:url),owner:restoreOwner)
            } catch { if (error as NSError).code != NSUserCancelledError { status = error.localizedDescription } }
        }
        .confirmationDialog("用本机备份写入该 WebDAV 文件？",isPresented:$confirmUpload,titleVisibility:.visible) {
            Button("写入备份") { transfer(upload:true) }
            Button("取消",role:.cancel) {}
        }
        .confirmationDialog("用备份中的 \(preview?.archive.semesters.count ?? 0) 张课表替换当前账号的课表与外观设置？",
                            isPresented:$confirmRestore,titleVisibility:.visible) {
            Button("恢复备份",role:.destructive) {
                guard let preview, restoreOwner == session.timetableOwner else { return }
                do { try preview.restore(owner:restoreOwner); status = "已恢复 \(preview.archive.semesters.count) 张课表"; self.preview = nil }
                catch { status = error.localizedDescription }
            }
            Button("取消",role:.cancel) { preview = nil }
        }
    }
    private func contents() throws -> Data {
        try JSONEncoder().encode(CampusBackup(archive:TimetableStore().load(account:session.timetableOwner)))
    }
    private func offer(_ bytes: Data, owner: String) throws {
        preview = try CampusBackup.read(bytes); restoreOwner = owner; confirmRestore = true
    }
    private func transfer(upload: Bool) {
        guard !busy else { return }
        busy = true; status = nil
        let owner = session.timetableOwner, remote = credential
        Task { @MainActor in
            defer { busy = false }
            do {
                let data = try await CampusDAV.transfer(remote,upload:upload ? contents() : nil)
                guard owner == session.timetableOwner else { return }
                if upload { status = "备份已上传" } else { try offer(data,owner:owner) }
            } catch { if owner == session.timetableOwner { status = error.localizedDescription } }
        }
    }
}
