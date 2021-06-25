namespace LED.Managers
{
#pragma warning disable SA1512
    using System;
    using System.Collections.Generic;
    using System.ComponentModel;
    using System.Diagnostics;
    using System.IO;
    using System.Net.Mail;
    using System.Text;
    using System.Threading;
    using System.Threading.Tasks;
    using LED.Common;
    using LED.Dao;
    using LED.Dao.Model;
    using LED.Enums;
    using LED.Model;
    using LED.Dao.Repository;

    public class EmailProcessor
    {
        private static Semaphore _semaphore; // will limit max number of emails to send simultaneously
        private SmtpConfiguration _smtpConf;
        private ISmtpClient _smtpClient;
        private EmailRecord _email; // guarda el objeto email a enviar en cada thread.

        //private bool _mailSent = false; indica si el intento de envío culminó
        private string _emailMessageHeaderTemplate = string.Empty;
        private string _emailMessageFooterTemplate = string.Empty;
        private IEmailRecordRepository _emailRecordRepository;

       public EmailProcessor(
            SmtpConfiguration conf,
            ISmtpClient client,
            EmailRecord mail,
            string headerTemplate,
            string footerTemplate,
            IEmailRecordRepository emailRecordRepo)
        {
            _smtpConf = conf;
            _smtpClient = client;
            _email = mail;
            _emailMessageHeaderTemplate = headerTemplate;
            _emailMessageFooterTemplate = footerTemplate;
            if (_semaphore == null)
            {
                _semaphore = new Semaphore(_smtpConf.MaxParallelsThread, _smtpConf.MaxParallelsThread);
            }

            _emailRecordRepository = emailRecordRepo;
        }

        private enum RecipientType
        {
            NORMAL,
            CC,
            BCC
        }

        public SendEmailResult SendEmail()
        {
            int initialUnixTS = (int)DateTime.UtcNow.Subtract(new DateTime(1970, 1, 1)).TotalSeconds;
            _semaphore.WaitOne((_smtpConf.MaxWaitingTimeForTurn + 5) * 1000); //esperamos un turno según lo configurado y un poco más!
            int diff = (int)DateTime.UtcNow.Subtract(new DateTime(1970, 1, 1)).TotalSeconds - initialUnixTS;
            SendEmailResult result;

            // Se procesan N correos en simultáneo. Si alguno debe esperar más de N segundos para obtener un turno, lo cancelamos para que no se recargue el sistema!
            if (diff > _smtpConf.MaxWaitingTimeForTurn)
            {
                this._email.Status = 0; //marcamos que no se envió!
                _emailRecordRepository.Update(_email);
                result = SendEmailResult.STARVATION;
            }
            else
            {
                MailMessage message = new MailMessage();
                string partialRecipients = string.Empty;
                try
                {
                    // Specify the email sender.
                    // Create a mailing address that includes a UTF8 character in the display name.
                    MailAddress from = new MailAddress(_smtpConf.Sender, this.ParseSenderAlias(), System.Text.Encoding.UTF8);
                    message.From = from;

                    // Message subject
                    message.Subject = _email.Subject == null ? string.Empty : _email.Subject;
                    message.SubjectEncoding = System.Text.Encoding.UTF8;

                    // Set destinations for the email message.
                    List<string> recipients = this.ParseRecipients(RecipientType.NORMAL);
                    this.AddRecipients(message, recipients);
                    partialRecipients = recipients.Count > 1 ? recipients.ToArray()[0] + ", " + recipients.ToArray()[1] + ", ..." : recipients.ToArray()[0];
                    this.AddCC(message, this.ParseRecipients(RecipientType.CC));
                    this.AddBCC(message, this.ParseRecipients(RecipientType.BCC));

                    // Message body construction
                    this.AssembleMailBody(message);

                    // Set attached files
                    this.AddAttachments(message, this.ParseAttachments());

                    // Set priority
                    switch (_email.PriorityForRecipient)
                    {
                        case 1:
                            message.Priority = MailPriority.High;
                            break;
                        case 2:
                            message.Priority = MailPriority.Normal;
                            break;
                        case 3:
                            message.Priority = MailPriority.Low;
                            break;
                        default:
                            message.Priority = MailPriority.Normal;
                            break;
                    }

                    var sw = new Stopwatch();
                    sw.Start();
                    var elapsed = 0d;

                    //------------------------------------
                    //USANDO UN CALLBACK QUE SE LLAMA AL CULMINAR (se complica si quiero encapsular el SmtpClient y usar una interface)
                    // Set the method that is called back when the send operation ends.
                    //smtpClient.SendCompleted += new SendCompletedEventHandler(SendCompletedCallback);

                    // The userState can be any object that allows your callback method to identify this send operation.
                    // For this example, the userToken is a string constant.
                    //string userState = DateTime.UtcNow.ToString();

                    //smtpClient.SendAsync(message, userState);

                    //while (!mailSent) { }
                    //------------------------------------

                    // este no devuelve la llamada a un método callback!
                    //igual debo usar Wait si quiero usar Dispose sin que arroje exception! (ya que continuaría el flujo)
                    _smtpClient.SendMailAsync(message).Wait();

                    // Clean up
                    message.Dispose();
                    _smtpClient.Dispose();

                    elapsed = sw.Elapsed.TotalMilliseconds;
                    sw.Reset();

                    Console.WriteLine($"{DateTime.UtcNow.ToString("yyyy-MM-dd HH:mm:ss")} | took {(int)(elapsed / 1000)}s | Subject: {message.Subject} | To: {partialRecipients}");
                    result = SendEmailResult.SUCCESSFUL;
                }
                catch (Exception ex)
                {
                    Console.WriteLine($"Exception while sending email! | Subject: {message.Subject} | To: {partialRecipients} | {ex.Message}");

                    //SI HUBO UN ERROR ENVIANDO EL CORREO, VUELVO A MODIFICAR EL ESTADO EN LA BBDD!!!
                    _email.Status = 0;
                    _emailRecordRepository.Update(_email);
                    result = SendEmailResult.ERROR;
                }
            }

            _semaphore.Release();

            return result;
        }

        //SI SE UTILIZA LA OPCIÓN DE SETEAR UN CALLBACK (smtpClient.SendCompleted += new SendCompletedEventHandler(SendCompletedCallback))
        private void SendCompletedCallback(object objeto, AsyncCompletedEventArgs e)
        {
            // objeto es el SmtpClient
            // Get the unique identifier for this asynchronous operation
            string token = (string)e.UserState;

            if (e.Cancelled)
            {
                Console.WriteLine("[{0}] Send canceled.", token);
            }

            if (e.Error != null)
            {
                Console.WriteLine("[{0}] {1}", token, e.Error.ToString());
            }
            else
            {
                //Console.WriteLine("Message sent!");
            }

            //this._mailSent = true;

            try
            {
                ((SmtpClient)objeto).Dispose();
            }
            catch (Exception)
            {
            }
        }

        private void AssembleMailBody(MailMessage mailMessage)
        {
            string messageBody = string.Empty;

            // HEADER
            messageBody += _emailMessageHeaderTemplate;

            // BODY
            messageBody += _email.MessageBody == null ? string.Empty : _email.MessageBody;

            // Include some non-ASCII characters in body and subject.
            string someArrows = new string(new char[] { '\u2190', '\u2191', '\u2192', '\u2193' });
            messageBody += Environment.NewLine + someArrows;

            // FOOTER
            messageBody += _emailMessageFooterTemplate;

            mailMessage.Body = messageBody;
            mailMessage.BodyEncoding = System.Text.Encoding.UTF8;
            mailMessage.IsBodyHtml = true;
        }

        private void AddRecipients(MailMessage message, List<string> recipients)
        {
            foreach (string recipient in recipients)
            {
                message.To.Add(recipient);
            }
        }

        private void AddCC(MailMessage message, List<string> cc)
        {
            if (cc != null)
            {
                foreach (string recipient in cc)
                {
                    message.CC.Add(recipient);
                }
            }
        }

        private void AddBCC(MailMessage message, List<string> bcc)
        {
            if (bcc != null)
            {
                foreach (string recipient in bcc)
                {
                    message.Bcc.Add(recipient);
                }
            }
        }

        private void AddAttachments(MailMessage message, List<string> files)
        {
            if (files != null)
            {
                foreach (string filePath in files)
                {
                    if (File.Exists(filePath))
                    {
                        message.Attachments.Add(new Attachment(filePath, "application/octet-stream"));
                    }
                }
            }
        }

        private string ParseSenderAlias()
        {
            if (_email.SenderShowAs == null || _email.SenderShowAs.Equals(string.Empty))
            {
                return _smtpConf.Sender;
            }

            return _email.SenderShowAs;

        }

        private List<string> ParseRecipients(RecipientType type)
        {
            List<string> recipients = new List<string>();
            string[] recipientsVec = null;
            switch (type)
            {
                case RecipientType.NORMAL:
                    if (_email.Recipients != null && !_email.Recipients.Equals(string.Empty))
                    {
                        recipientsVec = _email.Recipients.Split(" ", StringSplitOptions.RemoveEmptyEntries);
                    }

                    break;
                case RecipientType.CC:
                    if (_email.RecipientsCc != null && !_email.RecipientsCc.Equals(string.Empty))
                    {
                        recipientsVec = _email.RecipientsCc.Split(" ", StringSplitOptions.RemoveEmptyEntries);
                    }

                    break;
                case RecipientType.BCC:
                    if (_email.RecipientsBcc != null && !_email.RecipientsBcc.Equals(string.Empty))
                    {
                        recipientsVec = _email.RecipientsBcc.Split(" ", StringSplitOptions.RemoveEmptyEntries);
                    }

                    break;
            }

            if (recipientsVec != null)
            {
                foreach (var recipient in recipientsVec)
                {
                    recipients.Add(recipient);
                }
            }

            return recipients;
        }

        private List<string> ParseAttachments()
        {
            List<string> attachments = new List<string>();
            string[] attachmentsVec;

            if (_email.Attachments != null && _email.Attachments != string.Empty)
            {
                attachmentsVec = _email.Attachments.Split(" ", StringSplitOptions.RemoveEmptyEntries);

                foreach (var attachment in attachmentsVec)
                {
                    attachments.Add(attachment);
                }
            }

            return attachments;
        }
    }
}
