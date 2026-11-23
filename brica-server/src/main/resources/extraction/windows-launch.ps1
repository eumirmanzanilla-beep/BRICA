$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$java = Join-Path $root 'runtime\bin\java.exe'
$archive = Join-Path $root 'reader.jar'
$profileName = 'BRICA.Reader.' + [Guid]::NewGuid().ToString('N')

Add-Type -TypeDefinition @'
using System;
using System.Runtime.InteropServices;
using System.Text;
using System.Diagnostics;
using System.Threading.Tasks;

public static class BricaWindowsReader {
    [StructLayout(LayoutKind.Sequential)]
    struct Startup {
        public int Size;
        public IntPtr Reserved, Desktop, Title;
        public int X, Y, XSize, YSize, XCount, YCount, Fill, Flags;
        public short Show, ReservedSize;
        public IntPtr ReservedBytes, Input, Output, Error;
    }
    [StructLayout(LayoutKind.Sequential)]
    struct StartupEx { public Startup Startup; public IntPtr Attributes; }
    [StructLayout(LayoutKind.Sequential)]
    struct ProcessInfo { public IntPtr Process, Thread; public uint ProcessId, ThreadId; }
    [StructLayout(LayoutKind.Sequential)]
    struct Capabilities { public IntPtr Sid, List; public uint Count, Reserved; }
    [StructLayout(LayoutKind.Sequential)]
    struct BasicLimits {
        public long ProcessTime, JobTime;
        public uint Flags;
        public UIntPtr MinimumWorkingSet, MaximumWorkingSet;
        public uint ActiveProcesses;
        public UIntPtr Affinity;
        public uint PriorityClass, SchedulingClass;
    }
    [StructLayout(LayoutKind.Sequential)]
    struct Limits {
        public BasicLimits Basic;
        public ulong ReadOps, WriteOps, OtherOps, ReadBytes, WriteBytes, OtherBytes;
        public UIntPtr ProcessMemory, JobMemory, PeakProcessMemory, PeakJobMemory;
    }
    [DllImport("userenv.dll", CharSet=CharSet.Unicode)]
    static extern int CreateAppContainerProfile(string name, string display, string description, IntPtr caps, uint count, out IntPtr sid);
    [DllImport("userenv.dll", CharSet=CharSet.Unicode)]
    static extern int DeleteAppContainerProfile(string name);
    [DllImport("userenv.dll", CharSet=CharSet.Unicode)]
    static extern int GetAppContainerFolderPath(string sid, out IntPtr path);
    [DllImport("advapi32.dll")] static extern IntPtr FreeSid(IntPtr sid);
    [DllImport("advapi32.dll", SetLastError=true)] static extern bool OpenProcessToken(IntPtr process, uint access, out IntPtr token);
    [DllImport("advapi32.dll", SetLastError=true)]
    static extern bool GetTokenInformation(IntPtr token, int kind, out int value, int length, out int required);
    [DllImport("kernel32.dll", SetLastError=true)]
    static extern bool InitializeProcThreadAttributeList(IntPtr list, int count, uint flags, ref IntPtr size);
    [DllImport("kernel32.dll", SetLastError=true)]
    static extern bool UpdateProcThreadAttribute(IntPtr list, uint flags, IntPtr attribute, IntPtr value, IntPtr size, IntPtr previous, IntPtr returnSize);
    [DllImport("kernel32.dll")] static extern void DeleteProcThreadAttributeList(IntPtr list);
    [DllImport("kernel32.dll", CharSet=CharSet.Unicode, SetLastError=true)]
    static extern bool CreateProcessW(string application, StringBuilder command, IntPtr processAttributes, IntPtr threadAttributes,
        bool inheritHandles, uint flags, IntPtr environment, string directory, ref StartupEx startup, out ProcessInfo process);
    [DllImport("kernel32.dll", SetLastError=true)] static extern IntPtr CreateJobObject(IntPtr attributes, IntPtr name);
    [DllImport("kernel32.dll", SetLastError=true)] static extern bool SetInformationJobObject(IntPtr job, int kind, ref Limits limits, int size);
    [DllImport("kernel32.dll", SetLastError=true)]
    static extern bool IsProcessInJob(IntPtr process, IntPtr job, out bool result);
    [DllImport("kernel32.dll", SetLastError=true)] static extern uint ResumeThread(IntPtr thread);
    [DllImport("kernel32.dll", SetLastError=true)] static extern uint WaitForSingleObject(IntPtr handle, uint millis);
    [DllImport("kernel32.dll", SetLastError=true)] static extern bool GetExitCodeProcess(IntPtr process, out int result);
    [DllImport("kernel32.dll")] static extern bool TerminateProcess(IntPtr process, uint exitCode);
    [DllImport("kernel32.dll")] static extern bool CloseHandle(IntPtr handle);
    [DllImport("kernel32.dll")] static extern IntPtr GetCurrentProcess();
    [DllImport("kernel32.dll")] static extern IntPtr GetStdHandle(int id);
    [DllImport("kernel32.dll", SetLastError=true)]
    static extern bool DuplicateHandle(IntPtr source, IntPtr handle, IntPtr target, out IntPtr duplicate, uint access, bool inherit, uint flags);

    public static IntPtr Create(string name) {
        IntPtr sid;
        Marshal.ThrowExceptionForHR(CreateAppContainerProfile(name, "BRICA Source Reader", "Isolated Office document reader", IntPtr.Zero, 0, out sid));
        return sid;
    }
    public static string ProfileFolder(string sid) {
        IntPtr folder;
        Marshal.ThrowExceptionForHR(GetAppContainerFolderPath(sid, out folder));
        try { return Marshal.PtrToStringUni(folder); }
        finally { Marshal.FreeCoTaskMem(folder); }
    }
    public static void Delete(string name, IntPtr sid) {
        FreeSid(sid);
        Marshal.ThrowExceptionForHR(DeleteAppContainerProfile(name));
    }
    static void Require(bool ok, string stage) {
        if (!ok) throw new InvalidOperationException(stage + ":" + Marshal.GetLastWin32Error());
    }
    static IntPtr StandardHandle(int id) {
        IntPtr target;
        var self = GetCurrentProcess();
        Require(DuplicateHandle(self, GetStdHandle(id), self, out target, 0, true, 2), "STANDARD_PIPE");
        return target;
    }
    public static int Run(IntPtr sid, string exe, string command, string root, string environment) {
        IntPtr size = IntPtr.Zero;
        InitializeProcThreadAttributeList(IntPtr.Zero, 3, 0, ref size);
        if (size == IntPtr.Zero) throw new InvalidOperationException("ATTRIBUTE_SIZE");
        IntPtr list = IntPtr.Zero, caps = IntPtr.Zero, env = IntPtr.Zero, job = IntPtr.Zero, token = IntPtr.Zero, handles = IntPtr.Zero, jobs = IntPtr.Zero;
        IntPtr[] pipes = new IntPtr[3];
        ProcessInfo process = new ProcessInfo();
        bool initialized = false;
        try {
            list = Marshal.AllocHGlobal(size);
            Require(InitializeProcThreadAttributeList(list, 3, 0, ref size), "ATTRIBUTE_LIST");
            initialized = true;
            int capSize = Marshal.SizeOf(typeof(Capabilities));
            caps = Marshal.AllocHGlobal(capSize);
            Marshal.StructureToPtr(new Capabilities { Sid = sid }, caps, false);
            Require(UpdateProcThreadAttribute(list, 0, (IntPtr)0x20009, caps, (IntPtr)capSize, IntPtr.Zero, IntPtr.Zero), "APP_CONTAINER");
            handles = Marshal.AllocHGlobal(3 * IntPtr.Size);
            for (int i=0;i<3;i++) {
                pipes[i] = StandardHandle(-10-i);
                Marshal.WriteIntPtr(handles, i * IntPtr.Size, pipes[i]);
            }
            Require(UpdateProcThreadAttribute(list, 0, (IntPtr)0x20002, handles, (IntPtr)(3 * IntPtr.Size), IntPtr.Zero, IntPtr.Zero), "PIPE_HANDLES");
            job = CreateJobObject(IntPtr.Zero, IntPtr.Zero);
            Require(job != IntPtr.Zero, "JOB_CREATE");
            var limits = new Limits();
            limits.Basic.Flags = 0x2000 | 0x0200 | 0x0008 | 0x0002; // Kill on close, total commit, children, CPU time.
            limits.Basic.ActiveProcesses = 1;
            limits.Basic.ProcessTime = 60L * 10000000L;
            limits.JobMemory = (UIntPtr)(1024u * 1024u * 1024u);
            Require(SetInformationJobObject(job, 9, ref limits, Marshal.SizeOf(typeof(Limits))), "JOB_LIMITS");
            jobs = Marshal.AllocHGlobal(IntPtr.Size);
            Marshal.WriteIntPtr(jobs, job);
            Require(UpdateProcThreadAttribute(list, 0, (IntPtr)0x2000D, jobs, (IntPtr)IntPtr.Size, IntPtr.Zero, IntPtr.Zero), "JOB_LIST");
            var startup = new StartupEx { Attributes = list };
            startup.Startup.Size = Marshal.SizeOf(typeof(StartupEx));
            startup.Startup.Flags = 0x100;
            startup.Startup.Input=pipes[0]; startup.Startup.Output=pipes[1]; startup.Startup.Error=pipes[2];
            env = Marshal.StringToHGlobalUni(environment);
            Require(CreateProcessW(exe, new StringBuilder(command), IntPtr.Zero, IntPtr.Zero, true,
                0x08000000 | 0x00080000 | 0x00000400 | 0x00000004, env, root, ref startup, out process), "WORKER_START");
            Require(OpenProcessToken(process.Process, 8, out token), "TOKEN_QUERY");
            int isContainer, bytes;
            Require(GetTokenInformation(token, 29, out isContainer, sizeof(int), out bytes) && isContainer == 1, "TOKEN_NOT_CONFINED");
            bool inJob;
            Require(IsProcessInJob(process.Process, job, out inJob) && inJob, "JOB_VERIFY");
            Require(ResumeThread(process.Thread) != uint.MaxValue, "WORKER_RESUME");
            var cancellation = Task.Run(() => Console.OpenStandardInput().ReadByte());
            var timer = Stopwatch.StartNew();
            uint wait;
            while ((wait=WaitForSingleObject(process.Process, 250)) == 0x102) {
                if (cancellation.IsCompleted || timer.ElapsedMilliseconds >= 120000)
                    throw new InvalidOperationException("WORKER_CANCELLED_OR_LIMIT");
            }
            Require(wait == 0, "WORKER_WAIT");
            int exit;
            Require(GetExitCodeProcess(process.Process, out exit), "WORKER_EXIT");
            return exit;
        } finally {
            if (process.Process != IntPtr.Zero) { TerminateProcess(process.Process, 1); CloseHandle(process.Process); }
            if (process.Thread != IntPtr.Zero) CloseHandle(process.Thread);
            if (token != IntPtr.Zero) CloseHandle(token);
            if (job != IntPtr.Zero) CloseHandle(job);
            foreach (var pipe in pipes) if (pipe != IntPtr.Zero) CloseHandle(pipe);
            if (initialized) DeleteProcThreadAttributeList(list);
            Marshal.FreeHGlobal(list); Marshal.FreeHGlobal(caps); Marshal.FreeHGlobal(handles); Marshal.FreeHGlobal(env); Marshal.FreeHGlobal(jobs);
        }
    }
}
'@

$sid = [IntPtr]::Zero
$phase = 'PROFILE_CREATE'
try {
    $sid = [BricaWindowsReader]::Create($profileName)
    $principal = [Security.Principal.SecurityIdentifier]::new($sid)
    $phase = 'PROFILE_PATH'
    $profile = [BricaWindowsReader]::ProfileFolder($principal.Value)
    if (!(Test-Path -LiteralPath $profile -PathType Container)) { throw 'PROFILE_UNAVAILABLE' }
    # The Office reader needs no writable area. Deny writes to its own profile, too.
    $phase = 'PROFILE_ACL'
    $options = [Security.AccessControl.InheritanceFlags]'ContainerInherit,ObjectInherit'
    $rights = [Security.AccessControl.FileSystemRights]'Write,Delete,DeleteSubdirectoriesAndFiles,ChangePermissions,TakeOwnership'
    $deny = [Security.AccessControl.FileSystemAccessRule]::new($principal,$rights,$options,
        [Security.AccessControl.PropagationFlags]::None,[Security.AccessControl.AccessControlType]::Deny)
    $everyone = [Security.Principal.SecurityIdentifier]::new('S-1-1-0')
    $writeData = [Security.AccessControl.FileSystemRights]'WriteData,AppendData,WriteExtendedAttributes,WriteAttributes'
    $denyWrites = [Security.AccessControl.FileSystemAccessRule]::new($everyone,$writeData,$options,
        [Security.AccessControl.PropagationFlags]::None,[Security.AccessControl.AccessControlType]::Deny)
    $profileDirectories = [Collections.Generic.Queue[string]]::new()
    $profileDirectories.Enqueue($profile)
    while ($profileDirectories.Count -gt 0) {
        $directory = $profileDirectories.Dequeue()
        if (0 -ne ((Get-Item -LiteralPath $directory -Force).Attributes -band [IO.FileAttributes]::ReparsePoint)) {
            throw 'PROFILE_REPARSE_POINT'
        }
        foreach ($child in (Get-ChildItem -LiteralPath $directory -Directory -Force)) {
            if (0 -eq ($child.Attributes -band [IO.FileAttributes]::ReparsePoint)) {
                $profileDirectories.Enqueue($child.FullName)
            }
        }
        $acl = Get-Acl -LiteralPath $directory
        $acl.AddAccessRule($deny)
        $acl.AddAccessRule($denyWrites)
        Set-Acl -LiteralPath $directory -AclObject $acl
    }
    $phase = 'INPUT_ACL'
    $acl = Get-Acl -LiteralPath $root
    $readOnly = [Security.AccessControl.FileSystemAccessRule]::new($principal,
        [Security.AccessControl.FileSystemRights]::ReadAndExecute,$options,
        [Security.AccessControl.PropagationFlags]::None,[Security.AccessControl.AccessControlType]::Allow)
    $acl.AddAccessRule($readOnly)
    $acl.AddAccessRule($deny)
    Set-Acl -LiteralPath $root -AclObject $acl
    $envBlock = "BRICA_WINDOWS_READER_PROFILE=$profile`0BRICA_WINDOWS_SOURCE_SANDBOX=1`0LOCALAPPDATA=$env:LOCALAPPDATA`0PATH=$root\runtime\bin;$env:SystemRoot\System32`0SystemRoot=$env:SystemRoot`0TEMP=$root`0TMP=$root`0"
    $command = "`"$java`" -Xms32m -Xmx256m -XX:+UseSerialGC -Djava.awt.headless=true -Djava.io.tmpdir=`"$root`" -Duser.home=`"$root`" -Dloader.home=`"$root`" -Dloader.main=com.brica.extraction.infrastructure.worker.SourceWorker -cp `"$archive`" org.springframework.boot.loader.launch.PropertiesLauncher"
    $phase = 'WORKER'
    $exit = [BricaWindowsReader]::Run($sid, $java, $command, $root, $envBlock)
    if ($exit -ne 0) { throw 'READER_FAILED' }
} catch {
    # The backend never logs file names, document data, or JVM diagnostics from stderr.
    $code = $_.Exception.InnerException.Message
    if ($code -match '^[A-Z_]+:[0-9]+$') { $phase = $code }
    [Console]::Error.WriteLine("WINDOWS_READER_ISOLATION_OR_PROCESS_FAILED:$phase")
    exit 1
} finally {
    if ($sid -ne [IntPtr]::Zero) { [BricaWindowsReader]::Delete($profileName,$sid) }
}
