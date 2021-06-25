namespace LED.Managers
{
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
    using LED.Model;
    using LED.Dao.Repository;

    public class EmailsBatchProcessor
    {
        private SmtpConfiguration _smtpConf;
        private string _emailMessageHeaderTemplate = string.Empty;
        private string _emailMessageFooterTemplate = string.Empty;

        public EmailsBatchProcessor(
            SmtpConfiguration smtpConf,
            string headerTemplate,
            string footerTemplate)
        {
            this._smtpConf = smtpConf;
            this._emailMessageHeaderTemplate = headerTemplate;
            this._emailMessageFooterTemplate = footerTemplate;
        }

        public void SendEmailsBatch()
        {
            // RECUPERO LOTE DE LA BBDD Y LEVANTO UN THREAD POR CADA EMAIL
            List<EmailRecord> emailsBatch = this.GetEmailsBatchToProcess();

            // marcamos cada uno para que no los procese otro thread!
            using (var context = DbContextFactory.GetContext())
            {
                EmailRecordRepository err = new EmailRecordRepository(context);
                foreach (var email in emailsBatch)
                {
                    email.Status = 1;
                    err.Update(email);
                }
            }

            foreach (var email in emailsBatch)
            {
                SmtpClientImpl clientImpl = new SmtpClientImpl(new SmtpClient()
                {
                    UseDefaultCredentials = false,
                    EnableSsl = _smtpConf.ServerUseSSL,
                    Host = _smtpConf.ServerHost,
                    Port = _smtpConf.ServerPort,
                    Credentials = new System.Net.NetworkCredential(_smtpConf.Sender, _smtpConf.Password),
                    Timeout = _smtpConf.ConnTimeout
                });

                using (var context = DbContextFactory.GetContext())
                {
                    EmailRecordRepository err = new EmailRecordRepository(context);

                    EmailProcessor m = new EmailProcessor(
                    _smtpConf,
                    clientImpl,
                    email,
                    _emailMessageHeaderTemplate,
                    _emailMessageFooterTemplate,
                    err);

                    Thread t = new Thread(() => m.SendEmail());
                    t.Start();
                }
            }
        }

        private List<EmailRecord> GetEmailsBatchToProcess()
        {
            List<EmailRecord> emailsBatch = new List<EmailRecord>();

            try
            {
                using (var context = DbContextFactory.GetContext())
                {
                    EmailRecordRepository err = new EmailRecordRepository(context);
                    emailsBatch = err.GetEmailsBatchToProcess().Result;
                }
            }
            catch (Exception)
            {
            }

            return emailsBatch;
        }

    }
}
